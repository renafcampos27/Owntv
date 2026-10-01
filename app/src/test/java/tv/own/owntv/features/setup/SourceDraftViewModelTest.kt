package tv.own.owntv.features.setup

import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.settings.PlaylistRefresh

class SourceDraftViewModelTest {
    @Test fun sameFormAfterRecreationRetainsCredentialsAndOptions() {
        val vm = SourceDraftViewModel()
        val draft = vm.obtain("form-a", 1, null, PlaylistRefresh.OFF, false)
        draft.password.value = "private-draft"
        draft.m3uUrl.value = "https://example.invalid/list?token=private"
        draft.kind.value = SourceKind.M3U
        draft.preferHls.value = true
        val restored = vm.obtain("form-a", 1, null, PlaylistRefresh.OFF, false)
        assertSame(draft, restored)
        assertEquals("private-draft", restored.password.value)
        assertEquals(SourceKind.M3U, restored.kind.value)
        assertTrue(restored.preferHls.value)
    }

    @Test fun firstProfileEmissionBindsDraftWithoutDestroyingTypedText() {
        val vm = SourceDraftViewModel()
        val draft = vm.obtain("form-a", null, null, PlaylistRefresh.OFF, false)
        draft.name.value = "Draft"
        assertSame(draft, vm.obtain("form-a", 3, null, PlaylistRefresh.OFF, false))
    }

    @Test fun anotherProfileClearsOldSecretsAndStartsNewDraft() {
        val vm = SourceDraftViewModel()
        val old = vm.obtain("form-a", 1, null, PlaylistRefresh.OFF, false)
        old.password.value = "private"
        old.server.value = "https://example.invalid/secret"
        val next = vm.obtain("form-a", 2, null, PlaylistRefresh.OFF, false)
        assertNotSame(old, next)
        assertEquals("", old.password.value)
        assertEquals("", old.server.value)
        assertEquals("", next.password.value)
    }

    @Test fun cancelDiscardsDraftAndIndependentFormDoesNotInheritIt() {
        val vm = SourceDraftViewModel()
        val old = vm.obtain("form-a", 1, null, PlaylistRefresh.OFF, false)
        old.username.value = "private"
        assertEquals("", vm.obtain("form-b", 1, null, PlaylistRefresh.OFF, false).username.value)
        vm.discard("form-a")
        assertEquals("", old.username.value)
        assertNotSame(old, vm.obtain("form-a", 1, null, PlaylistRefresh.OFF, false))
    }
}
