package fr.julesdupont.portail

import fr.julesdupont.portail.Feedback.Event
import fr.julesdupont.portail.Feedback.Voice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedbackTest {
    private fun plan(event: Event, vibrate: Boolean = true, voice: Voice = Voice.HEADSET_ONLY,
                     headset: Boolean = true, inCall: Boolean = false) =
        Feedback.plan(event, vibrate, voice, headset, inCall)

    @Test
    fun `appel lance vibration courte sans annonce`() {
        val p = plan(Event.CALL_STARTED, inCall = true)
        assertEquals(Feedback.PATTERN_STARTED.toList(), p.vibration?.toList())
        assertNull(p.speech)
    }

    @Test
    fun `appel termine double vibration et annonce`() {
        val p = plan(Event.CALL_DONE)
        assertEquals(Feedback.PATTERN_DONE.toList(), p.vibration?.toList())
        assertEquals("Portail appelé", p.speech)
    }

    @Test
    fun `echec longue vibration et annonce`() {
        val p = plan(Event.CALL_FAILED)
        assertEquals(Feedback.PATTERN_FAILED.toList(), p.vibration?.toList())
        assertEquals("Le portail n'a pas pu être appelé", p.speech)
    }

    @Test
    fun `pas d'annonce sans ecouteurs par defaut`() =
        assertNull(plan(Event.CALL_DONE, headset = false).speech)

    @Test
    fun `annonce toujours meme sans ecouteurs si choisi`() =
        assertEquals("Portail appelé", plan(Event.CALL_DONE, voice = Voice.ALWAYS, headset = false).speech)

    @Test
    fun `annonce desactivee`() =
        assertNull(plan(Event.CALL_DONE, voice = Voice.OFF).speech)

    @Test
    fun `pas d'annonce pendant un appel`() =
        assertNull(plan(Event.CALL_DONE, inCall = true).speech)

    @Test
    fun `vibration desactivee`() =
        assertNull(plan(Event.CALL_DONE, vibrate = false).vibration)
}
