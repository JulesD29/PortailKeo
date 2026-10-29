package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Test

class SummariesTest {
    @Test
    fun jours() {
        assertEquals("Lun → ven", Summaries.days(setOf(1, 2, 3, 4, 5)))
        assertEquals("Tous les jours", Summaries.days((1..7).toSet()))
        assertEquals("Lun, mer, ven", Summaries.days(setOf(5, 1, 3)))
        assertEquals("Sam, dim", Summaries.days(setOf(6, 7)))
        assertEquals("Mar → jeu", Summaries.days(setOf(2, 3, 4)))
    }

    @Test
    fun horaires() = assertEquals("Lun → ven · 07:00–10:00", Summaries.schedule(setOf(1, 2, 3, 4, 5), 420, 600))

    @Test
    fun numero() {
        assertEquals("À renseigner", Summaries.phone(" "))
        assertEquals("0611223344", Summaries.phone("0611223344"))
    }

    @Test
    fun zones() {
        assertEquals("À placer sur la carte", Summaries.zone(false, 250, true, 1000))
        assertEquals("Zone du portail ${Zones.label(250)} · approche ${Zones.label(1000)}", Summaries.zone(true, 250, true, 1000))
        assertEquals("Zone du portail ${Zones.label(250)}", Summaries.zone(true, 250, false, 1000))
    }

    @Test
    fun appel() {
        assertEquals("Compte à rebours 5 s · raccroche après 20 s", Summaries.call(5, 20))
        assertEquals("Appel immédiat · pas de raccrochage automatique", Summaries.call(0, 0))
    }

    @Test
    fun retour() {
        assertEquals("Vibrations · annonce avec écouteurs", Summaries.feedback(true, Feedback.Voice.HEADSET_ONLY))
        assertEquals("Annonce vocale", Summaries.feedback(false, Feedback.Voice.ALWAYS))
        assertEquals("Désactivé", Summaries.feedback(false, Feedback.Voice.OFF))
    }

    @Test
    fun autorisations() {
        assertEquals("Tout est accordé", Summaries.permissions(0))
        assertEquals("1 réglage à faire", Summaries.permissions(1))
        assertEquals("3 réglages à faire", Summaries.permissions(3))
    }
}
