package fr.julesdupont.portail

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Historique des appels automatiques (une date-heure par ligne, plus récent en premier). */
object History {
    const val MAX = 60
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

    fun parse(text: String): List<LocalDateTime> =
        text.lines().mapNotNull { runCatching { LocalDateTime.parse(it.trim()) }.getOrNull() }

    fun add(text: String, at: LocalDateTime): String =
        (listOf(at.withNano(0).toString()) + text.lines().filter { it.isNotBlank() }).take(MAX).joinToString("\n")

    /** Nombre d'appels depuis lundi. */
    fun thisWeek(calls: List<LocalDateTime>, today: LocalDate): Int {
        val monday = today.with(DayOfWeek.MONDAY)
        return calls.count { !it.toLocalDate().isBefore(monday) && !it.toLocalDate().isAfter(today) }
    }

    /** Heure moyenne des [last] derniers appels, « 8 h 12 ». */
    fun averageTime(calls: List<LocalDateTime>, last: Int = 20): String? {
        val sample = calls.take(last).ifEmpty { return null }
        val avg = sample.map { it.hour * 60 + it.minute }.average().toInt()
        return "${avg / 60} h %02d".format(avg % 60)
    }

    fun summary(calls: List<LocalDateTime>, today: LocalDate): String {
        if (calls.isEmpty()) return "Aucun appel automatique pour l'instant"
        val week = thisWeek(calls, today)
        return "Cette semaine : $week appel${if (week > 1) "s" else ""} · arrivée moyenne ${averageTime(calls)}"
    }

    /** Une case de la semaine en cours (onglet Historique). */
    data class Day(val label: String, val time: String?, val isToday: Boolean, val future: Boolean)

    private val shortDays = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")

    /**
     * Semaine en cours, du lundi au dimanche : les jours actifs, plus ceux où un appel a eu lieu.
     * [Day.time] = heure du premier appel du jour (« 08:12 »), null sans appel.
     */
    fun week(calls: List<LocalDateTime>, today: LocalDate, activeDays: Set<Int>): List<Day> {
        val monday = today.with(DayOfWeek.MONDAY)
        return (0..6).mapNotNull { i ->
            val date = monday.plusDays(i.toLong())
            val first = calls.filter { it.toLocalDate() == date }.minOrNull()
            if (date.dayOfWeek.value !in activeDays && first == null) return@mapNotNull null
            Day(
                label = shortDays[i],
                time = first?.let { Rules.fmt(it.hour * 60 + it.minute) },
                isToday = date == today,
                future = date.isAfter(today),
            )
        }
    }

    /** « lun. 5 oct. · 08:12 » pour les [n] derniers appels. */
    fun recent(calls: List<LocalDateTime>, n: Int = 5): List<String> =
        calls.take(n).map { "${it.format(dayFmt)} · ${Rules.fmt(it.hour * 60 + it.minute)}" }
}
