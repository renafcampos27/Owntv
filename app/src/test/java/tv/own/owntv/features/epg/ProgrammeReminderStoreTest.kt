package tv.own.owntv.features.epg

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.nav.MainSection

class ProgrammeReminderStoreTest {

    private fun testChannel(id: Long = 10L, name: String = "RTP 1"): ChannelEntity =
        ChannelEntity(
            id = id,
            sourceId = 1L,
            name = name,
            streamUrl = "https://example.com/$id.m3u8",
        )

    private fun testProgramme(
        id: Long = 100L,
        title: String = "Telejornal",
        startMs: Long,
        stopMs: Long,
    ): EpgProgrammeEntity =
        EpgProgrammeEntity(
            id = id,
            sourceId = 1L,
            epgChannelId = "epg-10",
            title = title,
            startMs = startMs,
            stopMs = stopMs,
        )

    @Test
    fun `toggle schedules future programme and second toggle cancels it`() {
        val store = ProgrammeReminderStore()
        val now = 1_000_000L
        val channel = testChannel()
        val futureProg = testProgramme(startMs = now + 600_000L, stopMs = now + 2_400_000L)

        assertTrue(store.canSchedule(futureProg, now))
        assertTrue(store.toggle(profileId = 1L, channel = channel, programme = futureProg, nowMs = now))
        assertTrue(store.isScheduled(profileId = 1L, channelId = channel.id, startMs = futureProg.startMs))

        // Second toggle cancels the reminder
        assertFalse(store.toggle(profileId = 1L, channel = channel, programme = futureProg, nowMs = now))
        assertFalse(store.isScheduled(profileId = 1L, channelId = channel.id, startMs = futureProg.startMs))
    }

    @Test
    fun `past or currently airing programmes cannot be scheduled`() {
        val store = ProgrammeReminderStore()
        val now = 1_000_000L
        val channel = testChannel()
        val airingProg = testProgramme(startMs = now - 60_000L, stopMs = now + 600_000L)
        val pastProg = testProgramme(startMs = now - 1_200_000L, stopMs = now - 600_000L)

        assertFalse(store.canSchedule(airingProg, now))
        assertFalse(store.canSchedule(pastProg, now))
        assertFalse(store.toggle(profileId = 1L, channel = channel, programme = airingProg, nowMs = now))
        assertFalse(store.toggle(profileId = 1L, channel = channel, programme = pastProg, nowMs = now))
        assertTrue(store.reminders.value.isEmpty())
    }

    @Test
    fun `evaluateDue fires within lead window, respects profile isolation, and prunes expired items`() {
        val store = ProgrammeReminderStore()
        val baseNow = 10_000_000L
        val ch1 = testChannel(id = 1L, name = "RTP 1")
        val ch2 = testChannel(id = 2L, name = "SIC")
        val prog1 = testProgramme(id = 1L, title = "Jornal da Tarde", startMs = baseNow + 300_000L, stopMs = baseNow + 600_000L)
        val prog2 = testProgramme(id = 2L, title = "Primeiro Jornal", startMs = baseNow + 310_000L, stopMs = baseNow + 600_000L)

        store.toggle(profileId = 1L, channel = ch1, programme = prog1, nowMs = baseNow)
        store.toggle(profileId = 2L, channel = ch2, programme = prog2, nowMs = baseNow)

        // Before 60s lead window -> not due yet
        assertNull(store.evaluateDue(profileId = 1L, nowMs = prog1.startMs - ProgrammeReminderStore.LEAD_TIME_MS - 1L))

        // At exact 60s lead boundary -> due for profile 1, not for profile 3
        val dueAtLead = store.evaluateDue(profileId = 1L, nowMs = prog1.startMs - ProgrammeReminderStore.LEAD_TIME_MS)
        assertNotNull(dueAtLead)
        assertEquals(ch1.id, dueAtLead?.channelId)
        assertEquals("Jornal da Tarde", dueAtLead?.programmeTitle)
        assertNull(store.evaluateDue(profileId = 3L, nowMs = prog1.startMs))

        // Dismissing prog1 stops it from re-firing
        store.dismiss(dueAtLead!!.key)
        assertNull(store.evaluateDue(profileId = 1L, nowMs = prog1.startMs + 1_000L))

        // Profile 2's reminder is still intact until its stopMs passes
        assertNotNull(store.evaluateDue(profileId = 2L, nowMs = prog2.startMs))
        // After stopMs -> pruned automatically
        assertNull(store.evaluateDue(profileId = 2L, nowMs = prog2.stopMs))
        assertTrue(store.reminders.value.isEmpty())
    }

    @Test
    fun `evaluateDue auto-consumes reminder when user is already watching the target live channel`() {
        val store = ProgrammeReminderStore()
        val now = 5_000_000L
        val channel = testChannel(id = 7L, name = "TVI")
        val prog = testProgramme(title = "Jornal Nacional", startMs = now + 30_000L, stopMs = now + 1_800_000L)

        assertTrue(store.toggle(profileId = 1L, channel = channel, programme = prog, nowMs = now))

        // User is already watching channel 7 live at the live edge -> silently consumed, no popup returned
        val due = store.evaluateDue(profileId = 1L, nowMs = prog.startMs - 10_000L, playingLiveChannelId = 7L)
        assertNull(due)
        // Suppress the lead prompt but preserve it in case the user zaps away before the programme.
        assertTrue(store.isScheduled(profileId = 1L, channelId = 7L, startMs = prog.startMs))
        assertEquals(prog.startMs, store.evaluateDue(profileId = 1L, nowMs = prog.startMs, playingLiveChannelId = 8L)?.startMs)
        assertNull(store.evaluateDue(profileId = 1L, nowMs = prog.startMs, playingLiveChannelId = 7L))
        assertFalse(store.isScheduled(profileId = 1L, channelId = 7L, startMs = prog.startMs))
    }

    @Test
    fun `shouldProtectCurrentPlayback guards catch-up, recordings, timeshift, and VOD playback`() {
        // Player closed -> never protected
        assertFalse(
            ProgrammeReminderStore.shouldProtectCurrentPlayback(
                playerOpen = false,
                zapSource = MainSection.LIVE_TV,
                catchupActive = true,
                localCopyPlayback = true,
                timeshifted = true,
            ),
        )

        // Watching normal live TV at live edge -> not protected (Watch Channel can take primary focus)
        assertFalse(
            ProgrammeReminderStore.shouldProtectCurrentPlayback(
                playerOpen = true,
                zapSource = MainSection.LIVE_TV,
                catchupActive = false,
                localCopyPlayback = false,
                timeshifted = false,
            ),
        )

        // Catch-up archive playback -> protected
        assertTrue(
            ProgrammeReminderStore.shouldProtectCurrentPlayback(
                playerOpen = true,
                zapSource = MainSection.LIVE_TV,
                catchupActive = true,
                localCopyPlayback = false,
                timeshifted = false,
            ),
        )

        // Local recording / local timeshift copy playback -> protected
        assertTrue(
            ProgrammeReminderStore.shouldProtectCurrentPlayback(
                playerOpen = true,
                zapSource = MainSection.LIVE_TV,
                catchupActive = false,
                localCopyPlayback = true,
                timeshifted = false,
            ),
        )

        // Rewound live stream (timeshifted) -> protected
        assertTrue(
            ProgrammeReminderStore.shouldProtectCurrentPlayback(
                playerOpen = true,
                zapSource = MainSection.LIVE_TV,
                catchupActive = false,
                localCopyPlayback = false,
                timeshifted = true,
            ),
        )

        // Watching a Movie or Series episode -> protected
        assertTrue(
            ProgrammeReminderStore.shouldProtectCurrentPlayback(
                playerOpen = true,
                zapSource = MainSection.MOVIES,
                catchupActive = false,
                localCopyPlayback = false,
                timeshifted = false,
            ),
        )
    }

    @Test
    fun `re-scheduling a dismissed reminder clears dismissed state and selects earliest due first`() {
        val store = ProgrammeReminderStore()
        val now = 20_000_000L
        val ch1 = testChannel(id = 11L, name = "RTP 2")
        val ch2 = testChannel(id = 12L, name = "RTP 3")
        val laterProg = testProgramme(id = 1L, title = "Later", startMs = now + 50_000L, stopMs = now + 600_000L)
        val earlierProg = testProgramme(id = 2L, title = "Earlier", startMs = now + 20_000L, stopMs = now + 600_000L)

        // Invalid profile or degenerate programme bounds are rejected
        assertFalse(store.toggle(profileId = -1L, channel = ch1, programme = earlierProg, nowMs = now))
        val invalidBounds = testProgramme(id = 3L, title = "Invalid", startMs = now + 60_000L, stopMs = now + 60_000L)
        assertFalse(store.toggle(profileId = 1L, channel = ch1, programme = invalidBounds, nowMs = now))

        // Schedule both in reverse order; evaluateDue must return the earlier one first
        assertTrue(store.toggle(profileId = 1L, channel = ch1, programme = laterProg, nowMs = now))
        assertTrue(store.toggle(profileId = 1L, channel = ch2, programme = earlierProg, nowMs = now))

        val firstDue = store.evaluateDue(profileId = 1L, nowMs = now)
        assertEquals("Earlier", firstDue?.programmeTitle)

        // Dismiss the earlier reminder; the later one now surfaces
        store.dismiss(firstDue!!.key)
        assertEquals("Later", store.evaluateDue(profileId = 1L, nowMs = now)?.programmeTitle)

        // Re-scheduling the dismissed earlier reminder while still in the future makes it eligible again
        assertTrue(store.toggle(profileId = 1L, channel = ch2, programme = earlierProg, nowMs = now))
        assertEquals("Earlier", store.evaluateDue(profileId = 1L, nowMs = now)?.programmeTitle)
    }

    @Test
    fun `background lifecycle ON_STOP suppresses evaluation and auto-consumption until ON_START`() {
        val store = ProgrammeReminderStore()
        val scheduleTime = 30_000_000L
        val channel = testChannel(id = 7L, name = "TVI")
        val prog = testProgramme(
            id = 50L,
            title = "Jornal Nacional",
            startMs = scheduleTime + 120_000L,
            stopMs = scheduleTime + 1_800_000L,
        )

        assertTrue(store.toggle(profileId = 1L, channel = channel, programme = prog, nowMs = scheduleTime))

        // User presses Home / suspends box -> ON_STOP transitions lifecycle to CREATED
        val stoppedState = Lifecycle.Event.ON_STOP.targetState
        assertFalse(ProgrammeReminderStore.isForegroundLifecycle(stoppedState))
        assertFalse(ProgrammeReminderStore.canShowReminderDialog(stoppedState, overlaysClear = true))

        // Even if fullscreen flags still point to channel 7 while stopped, active live channel resolves to null
        val bgLiveChannelId = ProgrammeReminderStore.resolveActiveLiveEdgeChannelId(
            lifecycleState = stoppedState,
            playerFullscreen = true,
            multiviewOpen = false,
            zapSource = MainSection.LIVE_TV,
            enginePlaying = false,
            protectedPlayback = false,
            playingChannelId = 7L,
        )
        assertNull(bgLiveChannelId)

        // Reminder enters due window while app is in background: neither shown nor silently consumed
        val dueTimeInBg = prog.startMs - 30_000L
        assertNull(
            store.evaluateDue(
                profileId = 1L,
                nowMs = dueTimeInBg,
                playingLiveChannelId = 7L,
                lifecycleState = stoppedState,
            ),
        )
        assertTrue(store.isScheduled(profileId = 1L, channelId = 7L, startMs = prog.startMs))

        // User returns to the app -> ON_START transitions lifecycle to STARTED
        val startedState = Lifecycle.Event.ON_START.targetState
        assertTrue(ProgrammeReminderStore.isForegroundLifecycle(startedState))
        assertTrue(ProgrammeReminderStore.canShowReminderDialog(startedState, overlaysClear = true))

        // Before playback resumes on channel 7, the due reminder immediately surfaces on foreground return
        val dueOnResume = store.evaluateDue(
            profileId = 1L,
            nowMs = prog.startMs - 5_000L,
            playingLiveChannelId = null,
            lifecycleState = startedState,
        )
        assertNotNull(dueOnResume)
        assertEquals(prog.title, dueOnResume?.programmeTitle)

        // If the app is backgrounded again and only returns after stopMs, the expired reminder is pruned on ON_START
        assertNull(
            store.evaluateDue(
                profileId = 1L,
                nowMs = prog.stopMs + 1_000L,
                playingLiveChannelId = null,
                lifecycleState = stoppedState,
            ),
        )
        assertTrue(store.isScheduled(profileId = 1L, channelId = 7L, startMs = prog.startMs))
        assertNull(
            store.evaluateDue(
                profileId = 1L,
                nowMs = prog.stopMs + 1_000L,
                playingLiveChannelId = null,
                lifecycleState = startedState,
            ),
        )
        assertFalse(store.isScheduled(profileId = 1L, channelId = 7L, startMs = prog.startMs))
    }

    @Test
    fun `resolveActiveLiveEdgeChannelId requires both foreground lifecycle and active engine playback`() {
        val resumed = Lifecycle.Event.ON_RESUME.targetState

        // Foreground + fullscreen live TV + engine actively playing at live edge -> returns channel id
        assertEquals(
            7L,
            ProgrammeReminderStore.resolveActiveLiveEdgeChannelId(
                lifecycleState = resumed,
                playerFullscreen = true,
                multiviewOpen = false,
                zapSource = MainSection.LIVE_TV,
                enginePlaying = true,
                protectedPlayback = false,
                playingChannelId = 7L,
            ),
        )

        // Engine stopped or paused in foreground -> returns null so reminder is not silently consumed
        assertNull(
            ProgrammeReminderStore.resolveActiveLiveEdgeChannelId(
                lifecycleState = resumed,
                playerFullscreen = true,
                multiviewOpen = false,
                zapSource = MainSection.LIVE_TV,
                enginePlaying = false,
                protectedPlayback = false,
                playingChannelId = 7L,
            ),
        )

        // Protected playback (catch-up / timeshift / local recording) -> returns null
        assertNull(
            ProgrammeReminderStore.resolveActiveLiveEdgeChannelId(
                lifecycleState = resumed,
                playerFullscreen = true,
                multiviewOpen = false,
                zapSource = MainSection.LIVE_TV,
                enginePlaying = true,
                protectedPlayback = true,
                playingChannelId = 7L,
            ),
        )
    }
}
