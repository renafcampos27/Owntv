package tv.own.owntv

import org.junit.Assert.*
import org.junit.Test

class LaunchDeliveryTest {
    @Test fun recreationKeepsAConsumedLaunchButPreservesAnUndeliveredOne() {
        val launch = LaunchDelivery(); assertTrue(launch.consume(0))
        assertFalse(LaunchDelivery(launch.consumed).consume(0))
        assertTrue(LaunchDelivery(false).consume(0))
    }
    @Test fun sameChannelNewIntentIsNewAndLateConsumptionCannotClearIt() {
        val launch = LaunchDelivery(); val first = launch.receive(); val next = launch.receive()
        assertFalse(launch.consume(first)); assertFalse(launch.consumed)
        assertTrue(launch.consume(next)); assertFalse(launch.consume(next))
        assertTrue(launch.consume(launch.receive()))
    }
}
