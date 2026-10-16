// A source snapshot is the authoritative Core for this customized TV app.
// A local development override is allowed only when it matches the recorded build inputs.
val lockFile = settingsDir.resolve("core-source-lock.json")
check(lockFile.isFile) { "Missing customized Core lock. Run tools/core_snapshot/snapshot_core.py --core <checkout>." }
@Suppress("UNCHECKED_CAST")
val lock = groovy.json.JsonSlurper().parse(lockFile) as Map<String, Any>
val archive = settingsDir.resolve(lock["archive"] as String)
check(archive.isFile) { "Missing customized Core source snapshot: ${archive.name}" }
fun checksum(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }
val expectedHash = lock["sha256"] as String
check(checksum(archive.readBytes()) == expectedHash) { "Customized Core archive does not match core-source-lock.json." }
val coreOverride = if (providers.gradleProperty("owntvUseCoreSnapshot").orNull == "true") null
    else providers.gradleProperty("owntv.corePath").orNull?.takeIf { it.isNotBlank() }
val coreRoot = coreOverride?.let { settingsDir.resolve(it) }
    ?: settingsDir.resolve(".gradle/core-snapshot/$expectedHash/OwnTV_Core")
val rootPath = coreRoot.canonicalFile.toPath()
java.util.zip.ZipFile(archive).use { zip ->
    val entries = zip.entries().asSequence().filter { !it.isDirectory }.toList()
    val expectedPaths = entries.map { it.name }.toSet()
    for (entry in entries) {
        val destination = coreRoot.resolve(entry.name).canonicalFile
        check(destination.toPath().startsWith(rootPath)) { "Invalid Core archive entry" }
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        if (coreOverride == null && !destination.isFile) {
            destination.parentFile.mkdirs()
            destination.writeBytes(bytes)
        }
        check(destination.isFile && checksum(destination.readBytes()) == checksum(bytes)) {
            "Customized Core changed: ${entry.name}. Validate changes, then regenerate tools/core_snapshot/snapshot_core.py --core <checkout>."
        }
    }
    // Added source files must not silently exist only in the local build.
    val excluded = setOf("build", ".gradle", ".git", "__pycache__", "node_modules", "runs")
    val excludedNames = setOf("local.properties", "keystore.properties", "signing.properties", ".env")
    for (folder in listOf("core/src", "core/schemas", "player-core/src", "tools", "gradle")) {
        coreRoot.resolve(folder).walkTopDown().onEnter { it.name !in excluded }.filter { it.isFile }.forEach { path ->
            if (path.name !in excludedNames && path.extension !in setOf("pyc", "jks", "keystore", "log")) {
                val relative = path.relativeTo(coreRoot).invariantSeparatorsPath
                check(relative in expectedPaths) { "Core input is absent from the snapshot: $relative. Regenerate the validated snapshot." }
            }
        }
    }
}
// The cached composite needs this machine's SDK path, never a packaged developer path/credential.
if (coreOverride == null) {
    val appLocal = settingsDir.resolve("local.properties")
    val sdk = java.util.Properties().apply {
        if (appLocal.isFile) appLocal.inputStream().use { load(it) }
    }.getProperty("sdk.dir")
    if (!sdk.isNullOrBlank()) {
        val cachedLocal = coreRoot.resolve("local.properties")
        val local = java.util.Properties().apply {
            if (cachedLocal.isFile) cachedLocal.inputStream().use { load(it) }
        }
        if (local.getProperty("sdk.dir") != sdk) {
            java.util.Properties().apply { setProperty("sdk.dir", sdk) }
                .also { props -> cachedLocal.outputStream().use { props.store(it, "Local Android SDK only") } }
        }
    }
}
includeBuild(coreRoot) { name = "OwnTV_Core" }
logger.lifecycle("Customized Core ${lock["version"]}: source SHA-256 $expectedHash")
