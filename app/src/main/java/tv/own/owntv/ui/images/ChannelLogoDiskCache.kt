package tv.own.owntv.ui.images

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

internal data class LogoAsset(val file: File, val revision: String)

/** Private, derived thumbnails, outside Android's disposable cache and excluded from device backup. */
internal class ChannelLogoDiskCache(val root: File, private val maxBytes: Long = 64L * 1024 * 1024) {
    companion object {
        const val MAX_THUMBNAIL_BYTES = 192 * 1024
        const val FAILURE_BACKOFF_MS = 10 * 60_000L
        const val POLICY_VERSION = 1
        fun key(url: String): String = digest(url.toByteArray(Charsets.UTF_8))
        private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
    data class Record(val asset: LogoAsset?, val generation: Long, val attemptedGeneration: Long,
        val attemptedAt: Long, val etag: String?, val modified: String?, val retryAfterUntil: Long = 0,
        val policyVersion: Int = 0) {
        fun needsRefresh(generation: Long, now: Long): Boolean {
            if (now < retryAfterUntil) return false
            if (asset != null && this.generation >= generation) return false
            if (asset == null && policyVersion < POLICY_VERSION) return true
            if (asset == null && this.generation >= generation && this.generation == attemptedGeneration) return true
            return attemptedGeneration < generation || now < attemptedAt || now - attemptedAt >= FAILURE_BACKOFF_MS
        }
    }
    private var imageBytes: Long? = null
    init { require(maxBytes > 0) }

    @Synchronized fun read(key: String): Record {
        val properties = Properties()
        runCatching { File(root, "$key.properties").inputStream().use(properties::load) }
        val revision = properties.getProperty("revision")?.takeIf { it.matches(Regex("[a-f0-9]{64}")) }
        val format = properties.getProperty("format")?.takeIf { it in setOf("webp", "png") } ?: "webp"
        val file = revision?.let { File(root, "$key-$it.$format") }?.takeIf { it.isFile && it.length() in 1..MAX_THUMBNAIL_BYTES }
        return Record(file?.let { LogoAsset(it, revision) }, properties.getProperty("generation")?.toLongOrNull() ?: -1,
            properties.getProperty("attemptedGeneration")?.toLongOrNull() ?: -1,
            properties.getProperty("attemptedAt")?.toLongOrNull() ?: 0,
            properties.getProperty("etag"), properties.getProperty("modified"),
            properties.getProperty("retryAfterUntil")?.toLongOrNull() ?: 0,
            properties.getProperty("policyVersion")?.toIntOrNull() ?: 0)
    }

    /** Immutable content files + atomic metadata switch: failed refreshes cannot replace a valid logo. */
    @Synchronized fun save(key: String, bytes: ByteArray, generation: Long, now: Long, etag: String?, modified: String?): LogoAsset {
        require(bytes.size in 12..MAX_THUMBNAIL_BYTES)
        require(bytes.size <= maxBytes)
        val format = when {
            bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
                bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()) -> "webp"
            bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)) -> "png"
            else -> error("Unsupported logo thumbnail")
        }
        val revision = digest(bytes)
        val file = File(root, "$key-$revision.$format")
        if (!file.isFile) {
            val total = imageBytes ?: root.listFiles().orEmpty().filter(::isImage).sumOf { it.length() }
            atomic(file) { it.write(bytes) }
            imageBytes = total + bytes.size
        }
        metadata(key, revision, generation, generation, now, etag, modified, format = format)
        if ((imageBytes ?: 0) > maxBytes) {
            root.listFiles().orEmpty().filter { isImage(it) && it != file }.sortedBy { it.lastModified() }.forEach {
                if (imageBytes!! > maxBytes) { val size = it.length(); if (it.delete()) imageBytes = imageBytes!! - size }
            }
        }
        return LogoAsset(file, revision)
    }

    @Synchronized fun checked(key: String, record: Record, generation: Long, now: Long, success: Boolean,
        etag: String? = record.etag, modified: String? = record.modified, retryAfterUntil: Long = 0) {
        metadata(key, record.asset?.revision, if (success) generation else record.generation, generation, now, etag, modified,
            retryAfterUntil, record.asset?.file?.extension ?: "webp")
    }

    private fun metadata(key: String, revision: String?, generation: Long, attempted: Long, now: Long, etag: String?, modified: String?, retryAfterUntil: Long = 0, format: String = "webp") {
        val data = Properties().apply {
            revision?.let { setProperty("revision", it) }
            setProperty("generation", generation.toString()); setProperty("attemptedGeneration", attempted.toString())
            setProperty("attemptedAt", now.toString())
            setProperty("retryAfterUntil", retryAfterUntil.toString())
            setProperty("policyVersion", POLICY_VERSION.toString())
            setProperty("format", format)
            etag?.let { setProperty("etag", it) }; modified?.let { setProperty("modified", it) }
        }
        atomic(File(root, "$key.properties")) { data.store(it, null) }
    }

    private fun atomic(destination: File, write: (java.io.OutputStream) -> Unit) {
        check(root.mkdirs() || root.isDirectory)
        val temporary = File(root, UUID.randomUUID().toString() + ".part")
        try {
            temporary.outputStream().use { write(it); it.fd.sync() }
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }

    internal fun newDownload(): File {
        check(root.mkdirs() || root.isDirectory)
        return File.createTempFile("logo-", ".part", root)
    }

    private fun isImage(file: File): Boolean = file.extension == "webp" || file.extension == "png"

    /** Inactive/orphaned logos go first. A fixed disk ceiling still applies to unusually large lists. */
    @Synchronized fun prune(active: Set<String>): Set<String> {
        val removed = mutableSetOf<String>()
        val files = root.listFiles().orEmpty()
        val referenced = files.filter { it.extension == "properties" }.associate { it.nameWithoutExtension to read(it.nameWithoutExtension).asset?.file?.name }
        val images = files.filter(::isImage)
        var total = images.sumOf { it.length() }
        images.sortedWith(compareBy<File> { it.name != referenced[it.name.take(64)] || it.name.take(64) !in active }
            .reversed().thenBy { it.lastModified() }).forEach { file ->
            val key = file.name.take(64)
            val orphan = file.name != referenced[key]
            if (orphan || total > maxBytes) {
                val size = file.length()
                if (file.delete()) { total -= size; if (!orphan) removed.add(key) }
            }
        }
        files.filter { it.extension == "properties" && it.nameWithoutExtension !in active && read(it.nameWithoutExtension).asset == null }.forEach { it.delete() }
        // Stale staging files only; a live writer never loses its temporary file.
        files.filter { it.extension == "part" && System.currentTimeMillis() - it.lastModified() > 60 * 60_000L }.forEach { it.delete() }
        imageBytes = total
        return removed
    }

    @Synchronized fun invalidate(key: String, revision: String) {
        if (read(key).asset?.revision == revision) check(File(root, "$key.properties").delete())
    }
}
