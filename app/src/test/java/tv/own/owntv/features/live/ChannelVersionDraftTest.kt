package tv.own.owntv.features.live

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.ui.components.UiLoadState

class ChannelVersionDraftTest {
    private fun row(id: Long, name: String) = ChannelEntity(id = id, sourceId = 1,
        remoteId = id.toString(), name = name, streamUrl = "https://example.invalid/$id")

    @Test fun editingSurvivesAFailedRefreshAndSuccessfulRetryWithNewMetadata() {
        val full = row(1, "SIC Full HD")
        val hd = row(2, "SIC HD")
        val edited = listOf(hd, full)
        val failed = reconcileVersionDraft(UiLoadState.Failed(listOf(full, hd)), edited, dirty = true, reset = false)
        assertEquals(edited, failed)
        val loading = reconcileVersionDraft(UiLoadState.Loading(), failed, dirty = true, reset = false)
        assertEquals(edited, loading)
        val updatedHd = hd.copy(name = "SIC HD atualizado")
        val added = row(3, "SIC Low")
        val refreshed = reconcileVersionDraft(UiLoadState.Ready(listOf(full, updatedHd, added)), loading, dirty = true, reset = false)
        assertEquals(listOf(updatedHd, full, added), refreshed)
    }

    @Test fun automaticResetIsReappliedToAuthoritativeRefreshInsteadOfShowingAnOldManualOrder() {
        val low = row(1, "SIC Low")
        val full = row(2, "SIC Full HD")
        assertEquals(listOf(full, low), reconcileVersionDraft(UiLoadState.Ready(listOf(low, full)),
            previous = null, dirty = true, reset = true))
    }
}
