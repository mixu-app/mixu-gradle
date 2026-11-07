// Convention: reproducible archives.
//
// A build that produces different bytes from the same commit cannot be compared,
// so archive tasks get fixed timestamps and a stable file order.
plugins {
    base
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
