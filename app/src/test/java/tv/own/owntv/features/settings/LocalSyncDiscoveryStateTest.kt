package tv.own.owntv.features.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.own.owntv.core.sync.local.DiscoveredDevice

class LocalSyncDiscoveryStateTest {

    @Test
    fun `upsertDiscoveredDevice adds distinct devices and replaces re-announced device by name or id`() {
        val tv = DiscoveredDevice(name = "Living Room TV", address = "192.168.1.10", port = 8765, deviceId = "dev-tv")
        val phone = DiscoveredDevice(name = "Renato Phone", address = "192.168.1.25", port = 8765, deviceId = "dev-phone")

        var found = emptyList<DiscoveredDevice>()
        found = LocalSyncViewModel.upsertDiscoveredDevice(found, tv)
        found = LocalSyncViewModel.upsertDiscoveredDevice(found, phone)
        assertEquals(listOf(tv, phone), found)

        // Phone reconnects with a new DHCP lease IP: replaces previous entry rather than duplicating
        val phoneNewIp = DiscoveredDevice(name = "Renato Phone", address = "192.168.1.44", port = 8765, deviceId = "dev-phone")
        found = LocalSyncViewModel.upsertDiscoveredDevice(found, phoneNewIp)
        assertEquals(listOf(tv, phoneNewIp), found)
    }

    @Test
    fun `removeLostDevice removes matching service name and keeps remaining devices`() {
        val tv = DiscoveredDevice(name = "Living Room TV", address = "192.168.1.10", port = 8765, deviceId = "dev-tv")
        val phone = DiscoveredDevice(name = "Renato Phone", address = "192.168.1.25", port = 8765, deviceId = "dev-phone")
        val initial = listOf(tv, phone)

        val afterPhoneLost = LocalSyncViewModel.removeLostDevice(initial, "Renato Phone")
        assertEquals(listOf(tv), afterPhoneLost)

        val afterTvLost = LocalSyncViewModel.removeLostDevice(afterPhoneLost, "Living Room TV")
        assertTrue(afterTvLost.isEmpty())
    }
}
