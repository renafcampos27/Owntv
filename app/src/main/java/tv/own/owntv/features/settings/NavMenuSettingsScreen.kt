package tv.own.owntv.features.settings

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.R
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.nav.MainSection
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.NavDuotoneIcon
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * v4.3.0 — Nav menu customization. Two modes:
 * - **STATIC** (default): the user toggles which of the six browse icons show in the side rail.
 *   Settings is always pinned at the bottom and can never be hidden here.
 * - **DYNAMIC**: the icons adapt to what the active playlist actually contains (Home & Settings always
 *   show). The per-icon list is hidden in this mode — there's nothing to toggle.
 */
@Composable
fun NavMenuSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val settingsVm: SettingsViewModel = koinViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val simpleFlow = remember(context) { SimpleModePreferences.observe(context) }
    val simple by simpleFlow.collectAsStateWithLifecycle(initialValue = null)
    val autoStartConnected by tv.own.owntv.features.startup.AutoStartService.connected.collectAsStateWithLifecycle()
    val autoStartFailure by tv.own.owntv.features.startup.AutoStartService.failure.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mode by settingsVm.navMenuMode.collectAsStateWithLifecycle()
    val hidden by settingsVm.navMenuHidden.collectAsStateWithLifecycle()
    val colors = OwnTVTheme.colors

    val firstFocus = remember { FocusRequester() }
    var showModePicker by remember { mutableStateOf(false) }
    var showRecoveryTimeout by remember { mutableStateOf(false) }

    // Grab focus on the first row the moment the screen opens (mirrors VideoPlayerSettingsScreen).
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel()
            .focusProperties { onEnter = { runCatching { firstFocus.requestFocus() } } }
            .focusGroup()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Header(title = stringResource(R.string.settings_sidebar_customization), onBack = onBack)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_sidebar_description),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Row2(
            icon = OwnTVIcon.MENU,
            title = stringResource(R.string.settings_behavior),
            desc = when (mode) {
                SettingsRepository.NavMenuMode.DYNAMIC ->
                    stringResource(R.string.settings_nav_dynamic_description)
                SettingsRepository.NavMenuMode.STATIC ->
                    stringResource(R.string.settings_nav_static_description)
            },
            chip = stringResource(if (mode == SettingsRepository.NavMenuMode.DYNAMIC) R.string.settings_dynamic else R.string.settings_static),
            primaryChip = mode == SettingsRepository.NavMenuMode.DYNAMIC,
            chevron = true,
            onClick = { showModePicker = true },
            modifier = Modifier.focusRequester(firstFocus),
        )

        simple?.let { options ->
            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.channel_recovery),
                desc = stringResource(R.string.channel_recovery_description),
                chip = stringResource(if (options.channelRecovery) R.string.simple_mode_on else R.string.simple_mode_off),
                primaryChip = options.channelRecovery,
                onClick = { scope.launch { SimpleModePreferences.setChannelRecovery(context, !options.channelRecovery) } },
            )

            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.channel_recovery_timeout),
                desc = stringResource(R.string.channel_recovery_timeout_description),
                chip = stringResource(R.string.channel_recovery_seconds, options.recoveryTimeoutSeconds),
                chevron = true,
                onClick = { showRecoveryTimeout = true },
            )

            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.zapping_stop_clear),
                desc = stringResource(R.string.zapping_stop_clear_description),
                chip = stringResource(if (options.stopClearZapping) R.string.simple_mode_on else R.string.simple_mode_off),
                primaryChip = options.stopClearZapping,
                onClick = { scope.launch { SimpleModePreferences.setStopClearZapping(context, !options.stopClearZapping) } },
            )

            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.auto_start_boot),
                desc = stringResource(R.string.auto_start_boot_description),
                chip = stringResource(if (options.startOnBoot) R.string.simple_mode_on else R.string.simple_mode_off),
                primaryChip = options.startOnBoot,
                onClick = { scope.launch { SimpleModePreferences.setStartOnBoot(context, !options.startOnBoot) } },
            )

            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.auto_start_wake),
                desc = stringResource(R.string.auto_start_wake_description),
                chip = stringResource(if (options.startOnWake) R.string.simple_mode_on else R.string.simple_mode_off),
                primaryChip = options.startOnWake,
                onClick = { scope.launch { SimpleModePreferences.setStartOnWake(context, !options.startOnWake) } },
            )

            if (options.startOnBoot || options.startOnWake) {
                Row2(
                    icon = OwnTVIcon.MENU,
                    title = stringResource(R.string.auto_start_permission),
                    chip = stringResource(if (autoStartConnected) R.string.simple_mode_on else R.string.simple_mode_off),
                    primaryChip = autoStartConnected,
                    desc = autoStartFailure?.let { stringResource(R.string.auto_start_failure, it) }
                        ?: stringResource(R.string.auto_start_service_description),
                    onClick = {
                        runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { android.widget.Toast.makeText(context, R.string.auto_start_settings_unavailable, android.widget.Toast.LENGTH_LONG).show() }
                    },
                )
            }

            if ((options.startOnBoot || options.startOnWake) && !autoStartConnected) {
                Row2(
                    icon = OwnTVIcon.MENU,
                    title = stringResource(R.string.auto_start_app_info),
                    desc = stringResource(R.string.auto_start_restricted_help),
                    onClick = {
                        runCatching {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }.onFailure { android.widget.Toast.makeText(context, R.string.auto_start_settings_unavailable, android.widget.Toast.LENGTH_LONG).show() }
                    },
                )
            }

            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.simple_mode_categories),
                desc = stringResource(R.string.simple_mode_categories_description),
                chip = stringResource(if (options.hideCategories) R.string.simple_mode_on else R.string.simple_mode_off),
                primaryChip = options.hideCategories,
                onClick = { scope.launch { SimpleModePreferences.setHideCategories(context, !options.hideCategories) } },
            )
            Row2(
                icon = OwnTVIcon.MENU,
                title = stringResource(R.string.simple_mode_sidebar),
                desc = stringResource(R.string.simple_mode_sidebar_description),
                chip = stringResource(if (options.hideSidebar) R.string.simple_mode_on else R.string.simple_mode_off),
                primaryChip = options.hideSidebar,
                onClick = { scope.launch { SimpleModePreferences.setHideSidebar(context, !options.hideSidebar) } },
            )
        }

        // The per-icon toggle list only exists in STATIC mode. In DYNAMIC there's nothing to toggle —
        // the icons are decided by the playlist's content, so the list is hidden entirely.
        if (mode == SettingsRepository.NavMenuMode.STATIC) {
            Spacer(Modifier.height(10.dp))
            GroupLabel(stringResource(R.string.settings_icons))
            MainSection.browseOrder.forEach { section ->
                NavMenuRow(
                    section = section,
                    shown = section !in hidden,
                    onToggle = { settingsVm.setNavSectionHidden(section, hidden = section !in hidden) },
                )
            }
        }
    }

    if (showRecoveryTimeout) {
        PickerDialog(
            title = stringResource(R.string.channel_recovery_timeout),
            options = RecoveryTimeout.seconds.map { it.toString() to stringResource(R.string.channel_recovery_seconds, it) },
            selected = (simple?.recoveryTimeoutSeconds ?: RecoveryTimeout.DEFAULT_SECONDS).toString(),
            onSelect = { value ->
                scope.launch { SimpleModePreferences.setRecoveryTimeout(context, value.toInt()) }
                showRecoveryTimeout = false
            },
            onDismiss = { showRecoveryTimeout = false },
        )
    }

    if (showModePicker) {
        PickerDialog(
            title = stringResource(R.string.settings_nav_behavior),
            options = listOf(
                SettingsRepository.NavMenuMode.STATIC.name to stringResource(R.string.settings_static),
                SettingsRepository.NavMenuMode.DYNAMIC.name to stringResource(R.string.settings_dynamic),
            ),
            selected = mode.name,
            onSelect = { value ->
                settingsVm.setNavMenuMode(SettingsRepository.NavMenuMode.valueOf(value))
                showModePicker = false
            },
            onDismiss = { showModePicker = false },
        )
    }
}

/** One browse-icon row — toggles shown/hidden (STATIC mode only). */
@Composable
private fun NavMenuRow(
    section: MainSection,
    shown: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    FocusableSurface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        surface = GlassSurface.CARDS,
        contentAlignment = Alignment.CenterStart,
    ) { _ ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconTileSize)
                    .clip(RoundedCornerShape(Dimens.IconTileCorner))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                NavDuotoneIcon(
                    section = section,
                    color = if (shown) colors.onPrimaryContainer else colors.onPrimaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(section.labelRes), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(
                    if (shown) stringResource(R.string.settings_shown_in_menu) else stringResource(R.string.settings_hidden_from_menu),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            val bg = if (shown) colors.primaryContainer else colors.secondaryContainer
            val fg = if (shown) colors.onPrimaryContainer else colors.onSecondaryContainer
            Text(
                if (shown) stringResource(R.string.settings_shown) else stringResource(R.string.settings_hidden),
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
