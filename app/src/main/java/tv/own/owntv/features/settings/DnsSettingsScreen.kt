package tv.own.owntv.features.settings

import tv.own.owntv.core.R as CoreR

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.core.network.DohPresets
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.OwnTVTextField
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
fun DnsSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OwnTVTheme.colors
    val vm: SettingsViewModel = koinViewModel()

    val loadedConfig by vm.dnsEditorConfig.collectAsStateWithLifecycle()
    val dnsConfig = loadedConfig
    BackHandler { onBack() }
    if (dnsConfig == null) {
        Text(stringResource(CoreR.string.settings_loading), modifier = modifier.padding(40.dp))
        return
    }
    val dnsTestState by vm.dnsTest.collectAsStateWithLifecycle()

    var toggleOn by remember { mutableStateOf(dnsConfig.enabled) }
    var server by remember { mutableStateOf(tv.own.owntv.core.network.NetworkConfigInput.dnsText(dnsConfig)) }
    var attempted by remember { mutableStateOf(false) }
    val parsed = tv.own.owntv.core.network.NetworkConfigInput.dns(server, toggleOn)
    val serverConfigured = parsed != null
    val effectiveEnabled = toggleOn
    val dirty = if (toggleOn) parsed != dnsConfig else dnsConfig.enabled

    fun applyToggle(on: Boolean) {
        toggleOn = on
        attempted = false
        vm.resetDnsTest()
        if (!on) vm.saveDns(false, dnsConfig.host, dnsConfig.port, dnsConfig.dohUrl)
    }
    fun applySave() {
        attempted = true
        val cfg = parsed ?: return
        vm.saveDns(toggleOn, cfg.host, cfg.port, cfg.dohUrl)
        vm.resetDnsTest()
    }

    val toggleFocus = remember { FocusRequester() }
    val firstPresetFocus = remember { FocusRequester() }
    val serverFieldFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { toggleFocus.requestFocus() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel()
            .focusProperties { onEnter = { runCatching { toggleFocus.requestFocus() } } }
            .focusGroup()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Header(stringResource(CoreR.string.settings_dns), onBack)
        Spacer(Modifier.height(8.dp))

        GroupLabel(stringResource(CoreR.string.settings_dns_custom))
        Row2(
            icon = OwnTVIcon.DNS,
            title = stringResource(CoreR.string.settings_dns_use_custom),
            desc = stringResource(CoreR.string.settings_dns_toggle_description),
            chip = stringResource(if (effectiveEnabled) CoreR.string.common_on else CoreR.string.common_off),
            primaryChip = effectiveEnabled,
            modifier = Modifier
                .focusRequester(toggleFocus)
                .focusProperties { if (toggleOn) down = firstPresetFocus },
            onClick = { applyToggle(!toggleOn) },
        )

        // Red warning: toggle is on but no server configured
        if (toggleOn && attempted && !serverConfigured) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(tv.own.owntv.R.string.network_input_invalid),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFEF4444),
            )
        }

        // Simple conditional visibility — AnimatedVisibility interferes with D-pad focus on TV.
        if (toggleOn) {
            Column {
                Spacer(Modifier.height(12.dp))

                // DoH preset chips — first chip gets focus when toggle turns on.
                // Explicit vertical links: the 2D focus search otherwise jumps straight from the
                // preset row to the header / Save, skipping the toggle above and the field below.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.focusProperties {
                        up = toggleFocus
                        down = serverFieldFocus
                    },
                ) {
                    var first = true
                    for ((label, url) in DohPresets.all) {
                        val isActive = server.trim() == url
                        OwnTVButton(
                            label = label,
                            onClick = {
                                server = url
                                toggleOn = true
                                attempted = false
                                vm.resetDnsTest()
                            },
                            style = if (isActive) OwnTVButtonStyle.PRIMARY else OwnTVButtonStyle.SECONDARY,
                            modifier = if (first) {
                                first = false
                                Modifier.focusRequester(firstPresetFocus)
                            } else {
                                Modifier
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                OwnTVTextField(
                    value = server,
                    onValueChange = { server = it; attempted = false; vm.resetDnsTest() },
                    label = stringResource(CoreR.string.settings_dns_server),
                    placeholder = stringResource(CoreR.string.settings_dns_server_hint),
                    focusRequester = serverFieldFocus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties {
                            up = firstPresetFocus
                            down = saveFocus
                        },
                )

                Spacer(Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.focusProperties { up = serverFieldFocus },
                ) {
                    OwnTVButton(stringResource(CoreR.string.common_save), onClick = { applySave() }, modifier = Modifier.focusRequester(saveFocus))
                    OwnTVButton(
                        label = stringResource(
                            if (dnsTestState is SettingsViewModel.DnsTestState.Testing) CoreR.string.settings_testing
                            else CoreR.string.settings_dns_test,
                        ),
                        onClick = {
                            attempted = true
                            parsed?.let { vm.testDns(toggleOn, it.host, it.port, it.dohUrl) }
                        },
                        style = OwnTVButtonStyle.SECONDARY,
                    )
                    DnsTestLabel(dnsTestState)
                }
            }
        }

        if (dirty) Text(stringResource(tv.own.owntv.R.string.network_unsaved),
            style = MaterialTheme.typography.bodyMedium, color = colors.primary)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(CoreR.string.settings_dns_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(CoreR.string.settings_dns_limitations),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun DnsTestLabel(state: SettingsViewModel.DnsTestState) {
    val colors = OwnTVTheme.colors
    val (text, color) = when (state) {
        is SettingsViewModel.DnsTestState.Ok -> stringResource(
            CoreR.string.settings_dns_resolved,
            state.millis,
        ) to colors.primary
        is SettingsViewModel.DnsTestState.Fail -> state.failure.displayText() to Color(0xFFEF4444)
        else -> null to colors.onSurfaceVariant
    }
    if (text != null) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun SettingsViewModel.DnsTestFailure.displayText(): String = when (this) {
    SettingsViewModel.DnsTestFailure.ServerRequired -> stringResource(CoreR.string.settings_dns_enter_server)
    SettingsViewModel.DnsTestFailure.ServerNotReachable -> stringResource(CoreR.string.settings_dns_not_reachable)
    SettingsViewModel.DnsTestFailure.TimedOut -> stringResource(CoreR.string.settings_dns_timed_out)
    SettingsViewModel.DnsTestFailure.NetworkUnreachable -> stringResource(CoreR.string.settings_dns_network_unreachable)
    SettingsViewModel.DnsTestFailure.ConnectionRefused -> stringResource(CoreR.string.settings_dns_connection_refused)
    is SettingsViewModel.DnsTestFailure.NoAddresses -> stringResource(CoreR.string.settings_dns_no_addresses, host)
    is SettingsViewModel.DnsTestFailure.Unknown -> rawMessage
    SettingsViewModel.DnsTestFailure.Generic -> stringResource(CoreR.string.settings_dns_test_failed)
}
