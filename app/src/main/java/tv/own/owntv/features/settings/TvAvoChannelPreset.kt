package tv.own.owntv.features.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tv.own.owntv.core.customize.SectionCustomizations

/** Enable the existing policies; retain manual orders, group assignments and hidden versions. */
internal fun tvAvoChannelVersions(current: SectionCustomizations): SectionCustomizations =
    current.copy(groupChannelVersions = true, prioritizeChannelVersions = true)

/** Also covers an already-enabled mode after upgrade, restart, restore activation or profile change.
 * Reapply on activation/profile entry, not every preference edit: manual choices remain editable.
 */
internal suspend fun observeTvAvoChannelPreset(
    options: Flow<SimpleModeOptions>,
    profileIds: Flow<Long>,
    prepare: suspend (Long) -> Unit,
    enableRecovery: suspend () -> Unit,
) {
    combine(options, profileIds) { setting, id -> setting.tvAvoMode to id }
        .distinctUntilChanged()
        .collectLatest { (enabled, id) ->
            if (enabled && id >= 0) {
                prepare(id)
                enableRecovery()
            }
        }
}
