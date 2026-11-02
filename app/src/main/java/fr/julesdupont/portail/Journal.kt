package fr.julesdupont.portail

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    /** Couleur du point dans la frise : appel réussi, arrivée ou appel, information, problème. */
    enum class Kind { OK, CALL, INFO, PROBLEM }

    fun kind(message: String): Kind {
        val m = message.lowercase(Locale.FRENCH)
        return when {
            listOf("échec", "erreur", "occupée", "impossible", "n'est pas parti", "n'a pas pu", "indisponible", "manquante")
                .any { it in m } -> Kind.PROBLEM
            m.startsWith("raccroché") || m.startsWith("appel terminé") || m.startsWith("portail appelé") -> Kind.OK
            "appel" in m || "arrivée" in m -> Kind.CALL
            else -> Kind.INFO
        }
    }

    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

    /**
     * En-tête de jour : « Aujourd'hui », « Hier », « Lun. 26 oct. ».
     * [date] au format « dd/MM » (null = aujourd'hui) ; une date « dans le futur » est de l'année précédente.
     */
    fun dayLabel(date: String?, today: LocalDate): String {
        if (date == null) return "Aujourd'hui"
        val (d, m) = date.split("/").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return date
        val day = runCatching { LocalDate.of(today.year, m, d) }.getOrNull() ?: return date
        val real = if (day.isAfter(today)) day.minusYears(1) else day
        return when (real) {
            today -> "Aujourd'hui"
            today.minusDays(1) -> "Hier"
            else -> real.format(dayFmt).replaceFirstChar { it.uppercase() }
        }
    }
}
