// Convention: reproducible archives and pinned plugin resolution.
//
// A build that produces different bytes from the same commit cannot be compared,
// so archive tasks get fixed timestamps and a stable file order.
import java.text.SimpleDateFormat
import java.util.TimeZone

plugins {
    base
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val epoch = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
    timeZone = TimeZone.getTimeZone("UTC")
}.parse("2020-01-01T00:00:00Z")
