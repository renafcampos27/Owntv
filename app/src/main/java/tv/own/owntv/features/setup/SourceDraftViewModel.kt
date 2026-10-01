package tv.own.owntv.features.setup

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.model.SourceType
import tv.own.owntv.core.settings.PlaylistRefresh
import tv.own.owntv.core.sync.SyncScopeChoice

internal enum class SourceKind { XTREAM, M3U, STALKER }

/** In-memory only: URLs may contain credentials, so none of this draft goes into saved state. */
internal class SourceDraft(initial: SourceEntity?, refresh: PlaylistRefresh, default: Boolean) {
    val kind = mutableStateOf(when (initial?.type) {
        SourceType.M3U -> SourceKind.M3U
        SourceType.STALKER -> SourceKind.STALKER
        else -> SourceKind.XTREAM
    })
    val name = mutableStateOf(initial?.name ?: "")
    val server = mutableStateOf(initial?.takeIf { it.type == SourceType.XTREAM }?.url ?: "")
    val username = mutableStateOf(initial?.username ?: "")
    val password = mutableStateOf(initial?.password ?: "")
    val m3uUrl = mutableStateOf(initial?.takeIf { it.type == SourceType.M3U }?.url ?: "")
    val portalUrl = mutableStateOf(initial?.takeIf { it.type == SourceType.STALKER }?.url ?: "")
    val mac = mutableStateOf(initial?.mac ?: "")
    val serial = mutableStateOf(initial?.stalkerSerialNumber ?: "")
    val deviceId = mutableStateOf(initial?.stalkerDeviceId ?: "")
    val deviceId2 = mutableStateOf(initial?.stalkerDeviceId2 ?: "")
    val signature = mutableStateOf(initial?.stalkerSignature ?: "")
    val epgUrl = mutableStateOf(initial?.epgUrl ?: "")
    val userAgent = mutableStateOf(initial?.userAgent ?: "")
    val autoRefresh = mutableStateOf(refresh)
    val isDefault = mutableStateOf(default)
    val preferHls = mutableStateOf(initial?.preferHls == true)
    val syncLive = mutableStateOf(if (initial?.syncLive == false) SyncScopeChoice.Off else SyncScopeChoice.Now)
    val hasRemoteStalkerScopes = mutableStateOf(false)

    fun clear() {
        listOf(name, server, username, password, m3uUrl, portalUrl, mac, serial, deviceId,
            deviceId2, signature, epgUrl, userAgent).forEach { it.value = "" }
    }
}

/** Each visible form has its own opaque token; Activity recreation keeps its draft alive. */
internal class SourceDraftViewModel : ViewModel() {
    private data class Entry(var profile: Long?, val draft: SourceDraft)
    private val entries = mutableMapOf<String, Entry>()

    fun obtain(token: String, profile: Long?, initial: SourceEntity?, refresh: PlaylistRefresh, default: Boolean): SourceDraft {
        val previous = entries[token]
        if (previous != null && (profile == null || previous.profile == null || previous.profile == profile)) {
            if (profile != null) previous.profile = profile
            return previous.draft
        }
        previous?.draft?.clear()
        return SourceDraft(initial, refresh, default).also { entries[token] = Entry(profile, it) }
    }

    fun discard(token: String) { entries.remove(token)?.draft?.clear() }
    override fun onCleared() {
        entries.values.forEach { it.draft.clear() }
        entries.clear()
    }
}
