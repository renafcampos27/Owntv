package tv.own.owntv.features.shell.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.ArchivePlaybackConfig
import tv.own.owntv.features.settings.SettingsViewModel
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.theme.OwnTVTheme

/** Manual provider addresses, isolated to archives; configuring never probes the network. */
@Composable
internal fun ArchivePlaybackConfigDialog(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val sources by vm.sources.collectAsStateWithLifecycle()
    val configs by vm.archivePlaybackConfigs.collectAsStateWithLifecycle()
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf(false) }
    var channel by remember { mutableStateOf<ChannelEntity?>(null) }
    var query by remember { mutableStateOf("") }
    val results by produceState(emptyList<ChannelEntity>(), sourceId, query) {
        value = emptyList()
        val id = sourceId ?: return@produceState
        delay(250)
        value = vm.searchChannelPlaybackChannels(id, query)
    }
    var urls by remember { mutableStateOf(List(3) { "" }) }
    var manifest by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val colors = OwnTVTheme.colors
    fun back() {
        if (editing) editing = false
        else if (sourceId != null) { sourceId = null; query = "" }
        else onDismiss()
    }
    fun edit(target: ChannelEntity?) {
        channel = target
        val row = configs.firstOrNull { it.sourceId == sourceId &&
            if (target == null) it.isPlaylist else !it.isPlaylist && it.matches(target) }
        urls = List(3) { row?.templates?.getOrNull(it).orEmpty() }
        manifest = row?.manifestType
        error = false
        editing = true
    }
    BackHandler { back() }
    LaunchedEffect(sourceId, editing) { withFrameNanos { }; runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup(), contentAlignment = Alignment.Center) {
        Column(Modifier.dialogPanel(width = 600.dp, padding = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.archive_addresses_title), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            if (sourceId == null) {
                sources.forEachIndexed { index, source ->
                    OwnTVButton(source.name, onClick = { sourceId = source.id }, compact = true,
                        modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(focus) else Modifier))
                }
            } else if (!editing) {
                Text(sources.firstOrNull { it.id == sourceId }?.name.orEmpty(), color = colors.onSurface)
                OwnTVButton(stringResource(R.string.archive_addresses_playlist), onClick = { edit(null) }, compact = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus))
                OwnTVTextField(query, onValueChange = { query = it }, label = stringResource(R.string.archive_addresses_channel_search))
                results.forEach { target ->
                    OwnTVButton(target.name, onClick = { edit(target) }, compact = true, modifier = Modifier.fillMaxWidth())
                }
            } else {
                Text(channel?.name ?: stringResource(R.string.archive_addresses_playlist), color = colors.primary)
                Text(stringResource(R.string.archive_addresses_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(stringResource(R.string.archive_addresses_backup_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                urls.forEachIndexed { index, value ->
                    OwnTVTextField(value, onValueChange = { next ->
                        urls = urls.toMutableList().also { it[index] = next.take(ArchivePlaybackConfig.MAX_TEMPLATE_LENGTH) }
                        error = false
                    }, label = stringResource(R.string.archive_addresses_url, index + 1),
                        modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(focus) else Modifier))
                }
                Text(stringResource(R.string.archive_addresses_type), color = colors.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(null, "hls", "mpd").forEach { type ->
                        OwnTVButton(stringResource(when (type) { "hls" -> R.string.archive_addresses_hls; "mpd" -> R.string.archive_addresses_dash; else -> R.string.archive_addresses_auto }),
                            onClick = { manifest = type }, compact = true,
                            style = if (manifest == type) OwnTVButtonStyle.PRIMARY else OwnTVButtonStyle.SECONDARY)
                    }
                }
                if (error) Text(stringResource(R.string.archive_addresses_invalid), color = colors.onSurface)
                fun save(remove: Boolean) {
                    if (saving) return
                    val entries = if (remove) emptyList() else urls.map(String::trim).filter(String::isNotEmpty).distinct()
                    if (!remove && (entries.isEmpty() || entries.any { !ArchivePlaybackConfig.isValidTemplate(it) })) { error = true; return }
                    val id = sourceId ?: return
                    saving = true
                    scope.launch {
                        val ok = vm.saveArchivePlaybackConfig(ArchivePlaybackConfig(id, channel?.remoteId, channel?.name, entries, manifest))
                        saving = false
                        if (ok) editing = false else error = true
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OwnTVButton(stringResource(R.string.archive_addresses_save), onClick = { save(false) }, compact = true)
                    OwnTVButton(stringResource(R.string.archive_addresses_delete), onClick = { save(true) }, compact = true)
                }
            }
            OwnTVButton(stringResource(R.string.archive_addresses_back), onClick = { back() }, compact = true)
        }
    }
}
