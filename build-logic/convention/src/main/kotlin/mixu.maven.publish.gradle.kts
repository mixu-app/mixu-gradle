// SPDX-License-Identifier: Apache-2.0
//
// Convention: Maven Central Portal publishing with in-memory PGP signing and
// atomic bundle upload (`https://central.sonatype.com/api/v1/publisher/upload`).

import groovy.util.Node
import groovy.util.NodeList
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import java.util.UUID
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.signing.Sign

plugins {
    `maven-publish`
    signing
}

val stagingDir = layout.buildDirectory.dir("central-staging")
val bundleDir = layout.buildDirectory.dir("central-bundle")

val targetGroupId: String =
    providers
        .gradleProperty("mavenCentralGroupId")
        .orElse(providers.environmentVariable("MAVEN_CENTRAL_GROUP_ID"))
        .orElse("com.mixuread")
        .get()

publishing {
    repositories {
        maven {
            name = "CentralStaging"
            url = uri(stagingDir)
        }
    }

    publications.withType<MavenPublication>().configureEach {
        groupId = targetGroupId
        val pubName = name
        val javadocJar =
            tasks.register<Jar>("${pubName}JavadocJar") {
                archiveClassifier.set("javadoc")
                archiveAppendix.set(pubName)
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
            }
        artifact(javadocJar)

        pom {
            name.set(project.name)
            description.set(
                provider {
                    project.description ?: "Mixu open-source narrative graph component (${project.name})."
                },
            )
            url.set("https://github.com/mixu-app/${project.name}")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("mixu-app")
                    name.set("Mixu Contributors")
                    email.set("maintainers@mixuread.com")
                    organization.set("Mixu")
                    organizationUrl.set("https://mixuread.com")
                }
            }
            scm {
                url.set("https://github.com/mixu-app/${project.name}")
                connection.set("scm:git:https://github.com/mixu-app/${project.name}.git")
                developerConnection.set("scm:git:ssh://git@github.com/mixu-app/${project.name}.git")
            }
            withXml {
                val root = asNode()
                val dependenciesNodes = root.get("dependencies") as? NodeList ?: return@withXml
                for (depsObj in dependenciesNodes) {
                    val depsNode = depsObj as? Node ?: continue
                    for (depObj in depsNode.children()) {
                        val depNode = depObj as? Node ?: continue
                        val groupNodes = depNode.get("groupId") as? NodeList ?: continue
                        val groupNode = groupNodes.firstOrNull() as? Node ?: continue
                        if (groupNode.text() == "app.mixu") {
                            groupNode.setValue(targetGroupId)
                            val versionNodes = depNode.get("version") as? NodeList
                            val versionNode = versionNodes?.firstOrNull() as? Node
                            if (versionNode != null && versionNode.text().isNullOrBlank()) {
                                versionNode.setValue(project.version.toString())
                            } else if (versionNode == null) {
                                depNode.appendNode("version", project.version.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    doLast {
        val file = outputFile.get().asFile
        if (file.isFile) {
            val origGroup = project.group.toString()
            val updated =
                file
                    .readText(StandardCharsets.UTF_8)
                    .replace("\"group\": \"$origGroup\"", "\"group\": \"$targetGroupId\"")
                    .replace("\"group\": \"app.mixu\"", "\"group\": \"$targetGroupId\"")
            file.writeText(updated, StandardCharsets.UTF_8)
        }
    }
}

val signingKey: String? =
    providers
        .gradleProperty("signingInMemoryKey")
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey"))
        .orElse(providers.environmentVariable("GPG_SIGNING_KEY"))
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val signingPassword: String =
    providers
        .gradleProperty("signingInMemoryKeyPassword")
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"))
        .orElse(providers.environmentVariable("GPG_SIGNING_PASSWORD"))
        .orElse("")
        .get()

signing {
    isRequired = signingKey != null
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

val cleanCentralStaging =
    tasks.register<Delete>("cleanCentralStaging") {
        delete(stagingDir)
    }

tasks.matching { it.name == "publishAllPublicationsToCentralStagingRepository" }.configureEach {
    dependsOn(cleanCentralStaging)
}

val prepareCentralStagingChecksums =
    tasks.register("prepareCentralStagingChecksums") {
        group = "publishing"
        description = "Normalizes CentralStaging checksums (.md5, .sha1) and strips metadata files."
        dependsOn("publishAllPublicationsToCentralStagingRepository")

        doLast {
            val root = stagingDir.get().asFile
            if (!root.isDirectory) return@doLast
            root.walkTopDown().filter { it.isFile }.toList().forEach { file ->
                val name = file.name
                if (
                    name.startsWith("maven-metadata.xml") ||
                    name.endsWith(".asc.md5") ||
                    name.endsWith(".asc.sha1") ||
                    name.endsWith(".asc.sha256") ||
                    name.endsWith(".asc.sha512") ||
                    name.endsWith(".sha256") ||
                    name.endsWith(".sha512")
                ) {
                    file.delete()
                }
            }
            root.walkTopDown().filter { it.isFile }.toList().forEach { file ->
                val name = file.name
                if (!name.endsWith(".asc") && !name.endsWith(".md5") && !name.endsWith(".sha1")) {
                    val bytes = file.readBytes()
                    val md5Hex =
                        MessageDigest
                            .getInstance("MD5")
                            .digest(bytes)
                            .joinToString("") { "%02x".format(it) }
                    val sha1Hex =
                        MessageDigest
                            .getInstance("SHA-1")
                            .digest(bytes)
                            .joinToString("") { "%02x".format(it) }
                    java.io.File(file.parentFile, "$name.md5").writeText(md5Hex, StandardCharsets.UTF_8)
                    java.io.File(file.parentFile, "$name.sha1").writeText(sha1Hex, StandardCharsets.UTF_8)
                }
            }
        }
    }

val zipMavenCentralBundle =
    tasks.register<Zip>("zipMavenCentralBundle") {
        group = "publishing"
        description = "Packages CentralStaging repository artifacts into a Sonatype Central Portal bundle zip."
        dependsOn(prepareCentralStagingChecksums)
        from(stagingDir) {
            exclude("**/maven-metadata.xml*")
            exclude("**/*.asc.md5")
            exclude("**/*.asc.sha1")
            exclude("**/*.asc.sha256")
            exclude("**/*.asc.sha512")
        }
        destinationDirectory.set(bundleDir)
        archiveFileName.set(provider { "${project.name}-${project.version}-bundle.zip" })
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

tasks.register("publishAllPublicationsToMavenCentralRepository") {
    group = "publishing"
    description = "Uploads the signed Maven Central bundle zip to Sonatype Central Portal."
    dependsOn(zipMavenCentralBundle)

    doLast {
        val username =
            providers
                .gradleProperty("mavenCentralUsername")
                .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mavenCentralUsername"))
                .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
                .orNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Missing MAVEN_CENTRAL_USERNAME / mavenCentralUsername for Central Portal upload.")

        val password =
            providers
                .gradleProperty("mavenCentralPassword")
                .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mavenCentralPassword"))
                .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
                .orNull
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Missing MAVEN_CENTRAL_PASSWORD / mavenCentralPassword for Central Portal upload.")

        val zipFile = zipMavenCentralBundle.get().archiveFile.get().asFile
        check(zipFile.isFile && zipFile.length() > 0L) {
            "Central bundle zip does not exist or is empty: ${zipFile.absolutePath}"
        }

        val token =
            Base64
                .getEncoder()
                .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8))

        val publishingType =
            providers
                .gradleProperty("mavenCentralPublishingType")
                .orElse("AUTOMATIC")
                .get()

        val deploymentName = "$targetGroupId:${project.name}:${project.version}"
        val boundary = "----MixuCentralBoundary${UUID.randomUUID().toString().replace("-", "")}"
        val crlf = "\r\n"

        val headerPart =
            buildString {
                append("--").append(boundary).append(crlf)
                append("Content-Disposition: form-data; name=\"bundle\"; filename=\"")
                append(zipFile.name).append("\"").append(crlf)
                append("Content-Type: application/octet-stream").append(crlf)
                append(crlf)
            }.toByteArray(StandardCharsets.UTF_8)

        val footerPart = "$crlf--$boundary--$crlf".toByteArray(StandardCharsets.UTF_8)
        val fileBytes = Files.readAllBytes(zipFile.toPath())
        val bodyBytes = ByteArray(headerPart.size + fileBytes.size + footerPart.size)
        System.arraycopy(headerPart, 0, bodyBytes, 0, headerPart.size)
        System.arraycopy(fileBytes, 0, bodyBytes, headerPart.size, fileBytes.size)
        System.arraycopy(footerPart, 0, bodyBytes, headerPart.size + fileBytes.size, footerPart.size)

        val encodedName = URLEncoder.encode(deploymentName, StandardCharsets.UTF_8)
        val uploadUri =
            URI.create(
                "https://central.sonatype.com/api/v1/publisher/upload?publishingType=$publishingType&name=$encodedName",
            )

        val client =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build()

        val request =
            HttpRequest
                .newBuilder(uploadUri)
                .timeout(Duration.ofMinutes(3))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "multipart/form-data; boundary=$boundary")
                .header("Accept", "text/plain, application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() !in 200..299) {
            error(
                "Sonatype Central Portal upload failed with HTTP ${response.statusCode()}: ${response.body()}",
            )
        }
        val deploymentId = response.body().trim()
        logger.lifecycle("Uploaded $deploymentName to Sonatype Central Portal. Deployment ID: $deploymentId")
    }
}
