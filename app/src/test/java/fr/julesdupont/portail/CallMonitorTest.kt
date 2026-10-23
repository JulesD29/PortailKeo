package fr.julesdupont.portail

import fr.julesdupont.portail.CallMonitor.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

class CallMonitorTest {
    private fun decide(
        inCall: Boolean, seen: Boolean = inCall, elapsedS: Int, attempt: Int = 1,
        hangup: Int = 20, retryIfBusy: Boolean = false,
    ) = CallMonitor.decide(inCall, seen, elapsedS * 1000L, attempt, hangup, retryIfBusy)

    // ---------- Appel qui sonne normalement ----------

    @Test
    fun `appel qui sonne on attend jusqu'au raccrochage`() {
        assertEquals(Decision.WAIT, decide(inCall = true, elapsedS = 7))
        assertEquals(Decision.WAIT, decide(inCall = true, elapsedS = 19))
        assertEquals(Decision.HANG_UP, decide(inCall = true, elapsedS = 20))
    }

    @Test
    fun `raccrochage court avant la fenetre d'occupation`() =
        assertEquals(Decision.HANG_UP, decide(inCall = true, elapsedS = 5, hangup = 5))

    @Test
    fun `raccrochage desactive on arrete de suivre apres la fenetre d'occupation`() {
        assertEquals(Decision.WAIT, decide(inCall = true, elapsedS = 9, hangup = 0))
        assertEquals(Decision.RELEASE, decide(inCall = true, elapsedS = 10, hangup = 0))
    }

    @Test
    fun `appel termine apres avoir sonne pas de nouvel essai`() {
        assertEquals(Decision.ENDED, decide(inCall = false, seen = true, elapsedS = 12))
        assertEquals(Decision.ENDED, decide(inCall = false, seen = true, elapsedS = 12, retryIfBusy = true))
    }

    // ---------- Appel coupé sans sonner (jamais établi) ----------

    @Test
    fun `appel pas encore visible on attend la verification`() =
        assertEquals(Decision.WAIT, decide(inCall = false, seen = false, elapsedS = 3))

    @Test
    fun `appel coupe sans sonner on rappelle meme option decochee`() {
        assertEquals(Decision.RETRY, decide(inCall = false, seen = false, elapsedS = 6, attempt = 1))
        assertEquals(Decision.RETRY, decide(inCall = false, seen = false, elapsedS = 6, attempt = 2))
    }

    @Test
    fun `abandon apres le dernier essai`() =
        assertEquals(Decision.GIVE_UP, decide(inCall = false, seen = false, elapsedS = 6, attempt = CallMonitor.MAX_ATTEMPTS))

    // ---------- Ligne du portail occupée ----------

    @Test
    fun `ligne occupee option decochee on s'arrete sans rappeler`() {
        assertEquals(Decision.BUSY_STOP, decide(inCall = false, seen = true, elapsedS = 4))
        assertEquals(Decision.BUSY_STOP, decide(inCall = false, seen = true, elapsedS = 9))
    }

    @Test
    fun `ligne occupee option cochee on rappelle plus tard`() {
        assertEquals(Decision.RETRY_BUSY, decide(inCall = false, seen = true, elapsedS = 5, retryIfBusy = true))
        assertEquals(Decision.RETRY_BUSY, decide(inCall = false, seen = true, elapsedS = 5, attempt = 2, retryIfBusy = true))
    }

    @Test
    fun `ligne toujours occupee abandon apres le dernier essai`() =
        assertEquals(Decision.GIVE_UP,
            decide(inCall = false, seen = true, elapsedS = 5, attempt = CallMonitor.MAX_ATTEMPTS, retryIfBusy = true))
}
