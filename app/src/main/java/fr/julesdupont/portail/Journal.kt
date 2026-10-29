package fr.julesdupont.portail

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Lignes du journal (« 05/10 08:12:30  message ») découpées pour l'onglet Historique. */
object Journal {
    /** [date] vaut null pour un événement du jour. */
    data class Entry(val time: String, val date: String?, val message: String)

    private val line = Regex("^(\\d{2}/\\d{2}) (\\d{2}:\\d{2}):\\d{2}\\s+(.+)$")

    fun parse(logText: String, today: LocalDate): List<Entry> {
        val todayStamp = today.format(DateTimeFormatter.ofPattern("dd/MM"))
        return logText.lines().filter { it.isNotBlank() }.map { raw ->
            val m = line.find(raw.trim()) ?: return@map Entry("", null, raw.trim())
            val (date, time, msg) = m.destructured
            Entry(time, date.takeIf { it != todayStamp }, msg)
        }
    }
}
