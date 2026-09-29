package fr.julesdupont.portail

import fr.julesdupont.portail.Dashboard.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DashboardTest {
    /** Lundi 5 octobre 2026. */
    private fun at(h: Int, m: Int = 0, day: Int = 5, month: Int = 10) = LocalDateTime.of(2026, month, day, h, m)

    private val ok = RuleConfig(
        enabled = true, phone = "0611", days = setOf(1, 2, 3, 4, 5),
        startMinutes = 7 * 60, endMinutes = 10 * 60, pauseUntil = "", skipHolidays = true, lastCallDate = "",
    )

    private fun status(c: RuleConfig = ok, configured: Boolean = true, perms: Boolean = true, now: LocalDateTime = at(6)) =
        Dashboard.status(c, configured, perms, now)

    @Test
    fun `priorite des etats`() {
        assertEquals(Level.INCOMPLETE, status(configured = false, perms = false, c = ok.copy(enabled = false)).level)
        assertEquals(Level.WARNING, status(perms = false, c = ok.copy(enabled = false)).level)
        assertEquals(Level.OFF, status(c = ok.copy(enabled = false, pauseUntil = "2026-10-09")).level)
        assertEquals(Level.PAUSED, status(c = ok.copy(pauseUntil = "2026-10-09", lastCallDate = "2026-10-05")).level)
        assertEquals(Level.CALLED, status(c = ok.copy(lastCallDate = "2026-10-05")).level)
        assertEquals(Level.ACTIVE, status().level)
    }

    @Test
    fun `actif avant le creneau du jour`() =
        assertEquals("Prochain créneau : aujourd'hui 07:00–10:00", status(now = at(6, 30)).subtitle)

    @Test
    fun `actif pendant le creneau`() =
        assertEquals("Créneau en cours jusqu'à 10:00 : l'appel partira à votre arrivée", status(now = at(8)).subtitle)

    @Test
    fun `apres le creneau prochain jour`() =
        assertEquals("demain 07:00–10:00", Dashboard.nextWindow(ok, at(11)))

    @Test
    fun `vendredi soir prochain creneau lundi`() =
        assertEquals("lundi 12 oct. 07:00–10:00", Dashboard.nextWindow(ok, at(18, day = 9)))

    @Test
    fun `appele aujourd'hui prochain creneau demain`() {
        val s = status(c = ok.copy(lastCallDate = "2026-10-05"), now = at(8))
        assertEquals("Portail appelé aujourd'hui", s.title)
        assertEquals("Prochain créneau : demain 07:00–10:00", s.subtitle)
    }

    @Test
    fun `jours feries sautes`() {
        // Mercredi 11 novembre 2026 (Armistice) → jeudi 12.
        assertEquals("jeudi 12 nov. 07:00–10:00", Dashboard.nextWindow(ok, at(11, day = 10, month = 11)))
        val s = status(now = at(6, day = 11, month = 11))
        assertTrue(s.subtitle, s.subtitle.startsWith("Aujourd'hui férié (Armistice) · Prochain créneau : demain"))
    }

    @Test
    fun `pause affiche la date de reprise`() {
        val s = status(c = ok.copy(pauseUntil = "2026-10-07"), now = at(6))
        assertEquals(Level.PAUSED, s.level)
        assertEquals("En pause jusqu'au 07/10/2026 inclus\nReprise : jeudi 8 oct. 07:00–10:00", s.subtitle)
    }

    @Test
    fun `aucun jour actif`() {
        assertNull(Dashboard.nextWindow(ok.copy(days = emptySet()), at(6)))
        assertTrue(status(c = ok.copy(days = emptySet())).subtitle.startsWith("Aucun créneau"))
    }

    @Test
    fun `creneau qui passe minuit en cours`() =
        assertEquals("en cours jusqu'à 02:00",
            Dashboard.nextWindow(ok.copy(days = (1..7).toSet(), startMinutes = 22 * 60, endMinutes = 2 * 60), at(23)))

    @Test
    fun `dernier evenement du journal`() {
        val log = "05/10 08:12:45  Appel automatique lancé vers 0611\n05/10 08:12:40  Entrée dans la zone"
        assertEquals("05/10 08:12" to "Appel automatique lancé vers 0611", Dashboard.lastEvent(log))
        assertNull(Dashboard.lastEvent(""))
        assertNull(Dashboard.lastEvent("n'importe quoi"))
        assertTrue(Dashboard.isToday("05/10 08:12", LocalDate.of(2026, 10, 5)))
    }
}
