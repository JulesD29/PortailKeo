package fr.julesdupont.portail

import fr.julesdupont.portail.LiveCall.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveCallTest {
    @Test
    fun `rien a afficher au repos`() = assertNull(LiveCall.display(State.Idle))

    @Test
    fun `compte a rebours`() {
        val d = LiveCall.display(State.Countdown(remaining = 3, total = 5))!!
        assertEquals("3 s", d.big)
        assertEquals("Appel du portail dans 3 s", d.title)
        assertEquals("avancement de l'anneau", 40, d.progress)
        assertEquals(1, d.steps)
        assertEquals(LiveCall.Action.CANCEL, d.action)
    }

    @Test
    fun `fin du compte a rebours l'appel part`() {
        val d = LiveCall.display(State.Countdown(remaining = 0, total = 5))!!
        assertEquals("Appel du portail…", d.title)
        assertEquals(LiveCall.Action.NONE, d.action)
    }

    @Test
    fun `appel en cours avec raccrochage automatique`() {
        val d = LiveCall.display(State.Calling(elapsedMs = 12_000, hangupSeconds = 20, attempt = 1))!!
        assertEquals("Appel en cours · essai 1/3", d.badge)
        assertEquals("12 s", d.big)
        assertEquals("sur 20 s", d.small)
        assertEquals("Raccrochage automatique dans 8 s", d.subtitle)
        assertEquals(60, d.progress)
        assertEquals(2, d.steps)
        assertEquals(LiveCall.Action.HANG_UP, d.action)
    }

    @Test
    fun `appel en cours sans raccrochage automatique`() {
        val d = LiveCall.display(State.Calling(elapsedMs = 30_000, hangupSeconds = 0, attempt = 2))!!
        assertEquals("Pas de raccrochage automatique", d.subtitle)
        assertNull(d.progress)
        assertEquals("Appel en cours · essai 2/3", d.badge)
    }

    @Test
    fun `l'anneau ne depasse pas 100`() =
        assertEquals(100, LiveCall.display(State.Calling(25_000, 20, 1))!!.progress)

    @Test
    fun `nouvel essai ligne occupee`() {
        val d = LiveCall.display(State.Retrying(nextAttempt = 2, busy = true))!!
        assertEquals("Ligne du portail occupée", d.title)
        assertEquals("L'app rappelle dans 15 s", d.subtitle)
        assertEquals("Nouvel essai 2/3", d.badge)
    }

    @Test
    fun `termine toutes les etapes franchies`() {
        val d = LiveCall.display(State.Done)!!
        assertEquals(3, d.steps)
        assertEquals(LiveCall.Action.NONE, d.action)
    }
}
