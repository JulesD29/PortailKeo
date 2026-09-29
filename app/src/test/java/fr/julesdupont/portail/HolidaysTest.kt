package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HolidaysTest {

    @Test
    fun `date de Paques correcte sur plusieurs annees`() {
        mapOf(
            2024 to "2024-03-31", 2025 to "2025-04-20", 2026 to "2026-04-05",
            2027 to "2027-03-28", 2030 to "2030-04-21", 2038 to "2038-04-25",
        ).forEach { (year, expected) ->
            assertEquals("Pâques $year", LocalDate.parse(expected), Holidays.easter(year))
        }
    }

    @Test
    fun `jours feries fixes`() {
        mapOf(
            "2026-01-01" to "Jour de l'an", "2026-05-01" to "Fête du Travail",
            "2026-05-08" to "Victoire 1945", "2026-07-14" to "Fête nationale",
            "2026-08-15" to "Assomption", "2026-11-01" to "Toussaint",
            "2026-11-11" to "Armistice", "2026-12-25" to "Noël",
        ).forEach { (date, name) -> assertEquals(date, name, Holidays.name(LocalDate.parse(date))) }
    }

    @Test
    fun `jours feries mobiles 2026`() {
        assertEquals("Lundi de Pâques", Holidays.name(LocalDate.of(2026, 4, 6)))
        assertEquals("Ascension", Holidays.name(LocalDate.of(2026, 5, 14)))
        assertEquals("Lundi de Pentecôte", Holidays.name(LocalDate.of(2026, 5, 25)))
    }

    @Test
    fun `jours ouvres ne sont pas feries`() {
        listOf("2026-04-05", "2026-10-05", "2026-12-24", "2026-12-31").forEach {
            assertNull(it, Holidays.name(LocalDate.parse(it)))
        }
    }
}
