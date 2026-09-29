package fr.julesdupont.portail

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ce qu'affiche la carte d'état de l'écran principal (logique pure, testée à part). */
object Dashboard {
    enum class Level { ACTIVE, CALLED, PAUSED, OFF, WARNING, INCOMPLETE }

    data class Status(val level: Level, val title: String, val subtitle: String)

    private val dayFmt = DateTimeFormatter.ofPattern("EEEE d MMM", Locale.FRENCH)

    /**
     * @param configured numéro et position renseignés
     * @param permissionsOk localisation « toujours » et appels accordés
     */
    fun status(c: RuleConfig, configured: Boolean, permissionsOk: Boolean, now: LocalDateTime): Status {
        val today = now.toLocalDate()
        if (!configured) return Status(Level.INCOMPLETE, "Configuration incomplète",
            "Ajoutez le numéro et la position du portail")
        if (!permissionsOk) return Status(Level.WARNING, "Autorisations manquantes",
            "L'appel automatique ne peut pas fonctionner")
        if (!c.enabled) return Status(Level.OFF, "Appel automatique désactivé",
            "Activez-le pour appeler le portail à votre arrivée")
        Rules.pauseLabel(c.pauseUntil, today)?.let {
            return Status(Level.PAUSED, "En pause", it.replaceFirstChar { ch -> ch.uppercase() } +
                (nextWindow(c, now)?.let { w -> "\nReprise : $w" } ?: ""))
        }
        if (c.lastCallDate == today.toString()) {
            return Status(Level.CALLED, "Portail appelé aujourd'hui",
                nextWindow(c, now, skipToday = true)?.let { "Prochain créneau : $it" } ?: "Aucun créneau à venir")
        }
        val holiday = if (c.skipHolidays) Holidays.name(today) else null
        val next = nextWindow(c, now)
        val subtitle = when {
            next == null -> "Aucun créneau à venir : vérifiez les jours actifs"
            next.startsWith("en cours") -> "Créneau $next : l'appel partira à votre arrivée"
            else -> "Prochain créneau : $next"
        }
        return Status(Level.ACTIVE, "Actif", (holiday?.let { "Aujourd'hui férié ($it) · " } ?: "") + subtitle)
    }

    /**
     * Prochain créneau où un appel automatique est possible, en texte :
     * « en cours jusqu'à 10:00 », « aujourd'hui 07:00–10:00 », « demain 07:00–10:00 », « lundi 12 oct. 07:00–10:00 ».
     */
    fun nextWindow(c: RuleConfig, now: LocalDateTime, skipToday: Boolean = false): String? {
        val today = now.toLocalDate()
        val range = "${Rules.fmt(c.startMinutes)}–${Rules.fmt(c.endMinutes)}"
        val minutes = now.hour * 60 + now.minute
        for (offset in 0..14) {
            val date = today.plusDays(offset.toLong())
            if (offset == 0 && skipToday) continue
            if (date.dayOfWeek.value !in c.days) continue
            if (c.skipHolidays && Holidays.name(date) != null) continue
            if (Rules.pauseLabel(c.pauseUntil, date) != null) continue
            if (offset == 0) {
                if (Rules.inWindow(minutes, c.startMinutes, c.endMinutes)) return "en cours jusqu'à ${Rules.fmt(c.endMinutes)}"
                if (minutes < c.startMinutes) return "aujourd'hui $range"
                continue
            }
            return when (offset) {
                1 -> "demain $range"
                else -> "${date.format(dayFmt)} $range"
            }
        }
        return null
    }

    /** Dernière ligne du journal découpée en (« 05/10 08:12 », « message »). */
    fun lastEvent(logText: String): Pair<String, String>? {
        val line = logText.lineSequence().firstOrNull { it.isNotBlank() } ?: return null
        val match = Regex("^(\\d{2}/\\d{2} \\d{2}:\\d{2}):\\d{2}\\s+(.+)$").find(line) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    fun isToday(stamp: String, today: LocalDate): Boolean =
        stamp.startsWith(today.format(DateTimeFormatter.ofPattern("dd/MM")))
}
