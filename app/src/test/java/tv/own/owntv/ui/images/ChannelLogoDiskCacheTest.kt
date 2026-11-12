package tv.own.owntv.ui.images

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

internal fun webpFixture(value: Byte = 1): ByteArray = ByteArray(64) { value }.also {
    "RIFF".toByteArray().copyInto(it); "WEBP".toByteArray().copyInto(it, 8)
}

class ChannelLogoDiskCacheTest {
    private val directory = Files.createTempDirectory("channel-logo-test").toFile()
    private val key = ChannelLogoDiskCache.key("https://example.invalid/logo?password=private")

    @Test fun thumbnailAndValidatorsSurviveRepositoryRecreation() {
        val first = ChannelLogoDiskCache(directory)
        val asset = first.save(key, webpFixture(), 123, 10_000, "etag-1", "yesterday")
        val restored = ChannelLogoDiskCache(directory).read(key)
        assertEquals(asset, restored.asset); assertEquals("etag-1", restored.etag)
        assertEquals("yesterday", restored.modified); assertFalse(restored.needsRefresh(123, 20_000))
        assertTrue(restored.needsRefresh(124, 20_000))
        assertFalse(directory.walkTopDown().any { it.name.contains("password") || it.name.contains("private") })
    }

    @Test fun failedUpdateRetainsTheLastValidImageAndNextListGenerationRetries() {
        val cache = ChannelLogoDiskCache(directory)
        val asset = cache.save(key, webpFixture(), 100, 1_000, "tag", null)
        cache.checked(key, cache.read(key), 200, 2_000, false)
        val failed = cache.read(key)
        assertEquals(asset, failed.asset); assertEquals(100L, failed.generation)
        assertFalse(failed.needsRefresh(200, 2_001)); assertTrue(failed.needsRefresh(201, 2_001))
        assertTrue(failed.needsRefresh(200, 2_000 + ChannelLogoDiskCache.FAILURE_BACKOFF_MS))
    }

    @Test fun replacementIsImmutableAndUnchangedContentKeepsTheSameIdentity() {
        val cache = ChannelLogoDiskCache(directory)
        val first = cache.save(key, webpFixture(1), 1, 1, null, null)
        assertEquals(first, cache.save(key, webpFixture(1), 2, 2, null, null))
        val second = cache.save(key, webpFixture(2), 3, 3, null, null)
        assertNotEquals(first.file, second.file); assertTrue(first.file.isFile)
        assertArrayEquals(webpFixture(1), first.file.readBytes())
        cache.prune(setOf(key))
        assertFalse(first.file.exists()); assertTrue(second.file.exists())
    }

    @Test fun invalidThumbnailCannotReplaceTheLastValidEntry() {
        val cache = ChannelLogoDiskCache(directory)
        val first = cache.save(key, webpFixture(), 1, 1, null, null)
        assertTrue(runCatching { cache.save(key, "not an image".toByteArray(), 2, 2, null, null) }.isFailure)
        assertEquals(first, cache.read(key).asset)
        assertTrue(directory.listFiles().orEmpty().none { it.extension == "part" })
    }

    @Test fun diskCeilingAppliesDuringDownloadBatchesAndMissingFilesCanBeRepaired() {
        val cache = ChannelLogoDiskCache(directory, maxBytes = 128)
        for (i in 0..9) cache.save(ChannelLogoDiskCache.key("logo-$i"), webpFixture(), 1, i.toLong(), null, null)
        assertTrue(directory.listFiles().orEmpty().filter { it.extension == "webp" }.sumOf { it.length() } <= 128)
        val recentKey = ChannelLogoDiskCache.key("logo-9")
        val entry = cache.read(recentKey); assertNotNull(entry.asset)
        entry.asset!!.file.delete()
        val missing = cache.read(recentKey)
        assertNull(missing.asset)
        assertTrue(missing.needsRefresh(1, 10))
        assertTrue(missing.needsRefresh(1, 10_000_000))
    }

    @Test fun staleDecodeFailureCannotInvalidateANewerLogo() {
        val cache = ChannelLogoDiskCache(directory)
        val old = cache.save(key, webpFixture(1), 1, 1, null, null)
        val current = cache.save(key, webpFixture(2), 2, 2, null, null)
        cache.invalidate(key, old.revision); assertEquals(current, cache.read(key).asset)
        cache.invalidate(key, current.revision); assertNull(cache.read(key).asset)
    }

    @Test fun temporaryDirectoryIsCreatedOnlyByAnIoOperation() {
        val absent = directory.resolve("not-created-by-constructor")
        val cache = ChannelLogoDiskCache(absent)
        assertFalse(absent.exists()); assertNull(cache.read(key).asset)
        cache.save(key, webpFixture(), 1, 1, null, null)
        assertTrue(absent.isDirectory)
    }

    @Test fun pngFallbackSurvivesRestartAndParticipatesInTheSameDiskQuota() {
        val png = ByteArray(64) { 1 }.also { byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10).copyInto(it) }
        val cache = ChannelLogoDiskCache(directory, maxBytes = 128)
        val asset = cache.save(key, png, 1, 1, null, null)
        assertEquals("png", asset.file.extension)
        assertEquals(asset, ChannelLogoDiskCache(directory).read(key).asset)
        cache.checked(key, cache.read(key), 2, 2, false)
        assertEquals(asset, cache.read(key).asset)
        repeat(5) { cache.save(ChannelLogoDiskCache.key("extra-$it"), webpFixture(), 1, 3L + it, null, null) }
        assertTrue(directory.listFiles().orEmpty().filter { it.extension in setOf("png", "webp") }.sumOf { it.length() } <= 128)
    }

    @Test fun legacyFailureRetryNeverOverridesAnExplicitProviderRetryAfter() {
        val record = ChannelLogoDiskCache.Record(null, -1, 100, 1_000, null, null,
            retryAfterUntil = 60_000, policyVersion = 0)
        assertFalse(record.needsRefresh(100, 1_001))
        assertTrue(record.needsRefresh(100, 60_001))
        val current = record.copy(retryAfterUntil = 0, policyVersion = ChannelLogoDiskCache.POLICY_VERSION)
        assertFalse(current.needsRefresh(100, 1_001))
    }
}
