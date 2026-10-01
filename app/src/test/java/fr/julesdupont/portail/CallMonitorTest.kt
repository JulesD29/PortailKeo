package fr.julesdupont.portail

import fr.julesdupont.portail.CallMonitor.Decision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallMonitorTest {
    @Test
    fun `appel qui sonne on attend pour raccrocher`() =
        assertEquals(Decision.RINGING, CallMonitor.decide(inCall = true, attempt = 1))

    @Test
    fun `appel coupe sans sonner on rappelle`() {
        assertEquals(Decision.RETRY, CallMonitor.decide(inCall = false, attempt = 1))
        assertEquals(Decision.RETRY, CallMonitor.decide(inCall = false, attempt = 2))
    }

    @Test
    fun `abandon apres le dernier essai`() =
        assertEquals(Decision.GIVE_UP, CallMonitor.decide(inCall = false, attempt = CallMonitor.MAX_ATTEMPTS))

    @Test
    fun `delai de raccrochage`() {
        assertEquals(14_000L, CallMonitor.hangupDelayMs(20))   // 20 s - 6 s de vérification
        assertEquals(1_000L, CallMonitor.hangupDelayMs(5))     // jamais moins d'1 s
        assertNull(CallMonitor.hangupDelayMs(0))               // 0 = ne pas raccrocher
    }
}
