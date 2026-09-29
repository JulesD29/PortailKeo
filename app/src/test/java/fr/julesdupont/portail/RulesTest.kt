package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class RulesTest {
    /** Lundi 5 octobre 2026, 8h00 : jour ouvré, dans la plage par défaut. */
    private val monday8h = LocalDateTime.of(2026, 10, 5, 8, 0)

    private val ok = RuleConfig(
        enabled = true, phone = "0600000000", days = setOf(1, 2, 3, 4, 5),
        startMinutes = 7 * 60, endMinutes = 10 * 60,
        pauseUntil = "", skipHolidays = true, lastCallDate = "",
    )

    private fun refusal(c: RuleConfig = ok, at: LocalDateTime = monday8h) = Rules.evaluate(c, at)

    @Test
    fun `appel autorise dans les conditions normales`() = assertNull(refusal())

    @Test
    fun `automatisation desactivee`() =
        assertEquals("automatisation désactivée", refusal(ok.copy(enabled = false)))

    @Test
    fun `numero manquant`() =
        assertEquals("aucun numéro configuré", refusal(ok.copy(phone = "  ")))

    @Test
    fun `samedi hors jours actifs`() =
        assertEquals("jour non actif", refusal(at = LocalDateTime.of(2026, 10, 10, 8, 0)))

    @Test
    fun `bornes de la plage horaire incluses`() {
        assertNull(refusal(at = monday8h.withHour(7).withMinute(0)))
        assertNull(refusal(at = monday8h.withHour(10).withMinute(0)))
        assertTrue(refusal(at = monday8h.withHour(6).withMinute(59))!!.startsWith("hors plage horaire"))
        assertTrue(refusal(at = monday8h.withHour(10).withMinute(1))!!.startsWith("hors plage horaire"))
    }

    @Test
    fun `plage horaire qui passe minuit`() {
        assertTrue(Rules.inWindow(23 * 60 + 30, 22 * 60, 2 * 60))
        assertTrue(Rules.inWindow(60, 22 * 60, 2 * 60))
        assertFalse(Rules.inWindow(12 * 60, 22 * 60, 2 * 60))
    }

    @Test
    fun `pause active jusqu'au dernier jour inclus`() {
        val paused = ok.copy(pauseUntil = "2026-10-05")
        assertTrue(refusal(paused)!!.startsWith("en pause jusqu'au 05/10/2026"))
        assertNull(refusal(paused, LocalDateTime.of(2026, 10, 6, 8, 0)))
    }

    @Test
    fun `pause passee ou invalide ignoree`() {
        assertNull(refusal(ok.copy(pauseUntil = "2026-10-01")))
        assertNull(refusal(ok.copy(pauseUntil = "n'importe quoi")))
        assertNull(Rules.pauseLabel("", LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `jour ferie bloque sauf si l'option est desactivee`() {
        val ascension = LocalDateTime.of(2026, 5, 14, 8, 0)
        assertEquals("jour férié (Ascension)", refusal(at = ascension))
        assertNull(refusal(ok.copy(skipHolidays = false), ascension))
    }

    @Test
    fun `un seul appel automatique par jour`() {
        assertEquals("déjà appelé aujourd'hui", refusal(ok.copy(lastCallDate = "2026-10-05")))
        assertNull(refusal(ok.copy(lastCallDate = "2026-10-04")))
    }

    @Test
    fun `desactivation prioritaire sur les autres raisons`() =
        assertEquals("automatisation désactivée",
            refusal(ok.copy(enabled = false, lastCallDate = "2026-10-05"), LocalDateTime.of(2026, 5, 14, 8, 0)))

    @Test
    fun `format des heures`() {
        assertEquals("07:05", Rules.fmt(7 * 60 + 5))
        assertEquals("23:59", Rules.fmt(23 * 60 + 59))
    }
}
