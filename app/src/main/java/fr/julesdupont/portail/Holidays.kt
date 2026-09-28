package fr.julesdupont.portail

import java.time.LocalDate
import java.time.MonthDay

/** Jours fériés de France métropolitaine. */
object Holidays {
    private val fixed = mapOf(
        MonthDay.of(1, 1) to "Jour de l'an",
        MonthDay.of(5, 1) to "Fête du Travail",
        MonthDay.of(5, 8) to "Victoire 1945",
        MonthDay.of(7, 14) to "Fête nationale",
        MonthDay.of(8, 15) to "Assomption",
        MonthDay.of(11, 1) to "Toussaint",
        MonthDay.of(11, 11) to "Armistice",
        MonthDay.of(12, 25) to "Noël",
    )

    /** Dimanche de Pâques (algorithme de Meeus/Jones/Butcher). */
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /** Nom du jour férié, ou null si jour ouvré. */
    fun name(date: LocalDate): String? {
        fixed[MonthDay.from(date)]?.let { return it }
        val easter = easter(date.year)
        return when (date) {
            easter.plusDays(1) -> "Lundi de Pâques"
            easter.plusDays(39) -> "Ascension"
            easter.plusDays(50) -> "Lundi de Pentecôte"
            else -> null
        }
    }
}
