package fr.julesdupont.portail

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Réglages utiles aux règles, séparés de SharedPreferences pour être testés facilement. */
data class RuleConfig(
    val enabled: Boolean,
    val phone: String,
    val days: Set<Int>,
    val startMinutes: Int,
    val endMinutes: Int,
    val pauseUntil: String,
    val skipHolidays: Boolean,
    val lastCallDate: String,
)

fun Prefs.toRuleConfig() = RuleConfig(
    enabled = enabled,
    phone = phone,
    days = days,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    pauseUntil = pauseUntil,
    skipHolidays = skipHolidays,
    lastCallDate = lastCallDate,
)

object Rules {
    private val dayFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Retourne null si l'appel automatique doit être passé, sinon la raison du refus. */
    fun check(p: Prefs, now: LocalDateTime = AppClock.now()): String? = evaluate(p.toRuleConfig(), now)

    /** Règles, dans l'ordre de priorité des messages. */
    fun evaluate(c: RuleConfig, now: LocalDateTime): String? {
        val today = now.toLocalDate()
        if (!c.enabled) return "automatisation désactivée"
        if (c.phone.isBlank()) return "aucun numéro configuré"
        pauseLabel(c.pauseUntil, today)?.let { return it }
        if (c.skipHolidays) Holidays.name(today)?.let { return "jour férié ($it)" }
        if (now.dayOfWeek.value !in c.days) return "jour non actif"
        if (!inWindow(now.hour * 60 + now.minute, c.startMinutes, c.endMinutes)) {
            return "hors plage horaire (${fmt(c.startMinutes)}–${fmt(c.endMinutes)})"
        }
        if (c.lastCallDate == today.toString()) return "déjà appelé aujourd'hui"
        return null
    }

    /** Plage [start, end] incluse ; si start > end, la plage passe minuit. */
    fun inWindow(minutes: Int, start: Int, end: Int): Boolean =
        if (start <= end) minutes in start..end else (minutes >= start || minutes <= end)

    fun pauseLabel(p: Prefs, today: LocalDate = AppClock.today()): String? = pauseLabel(p.pauseUntil, today)

    /** "en pause jusqu'au …" si une pause est en cours (date incluse), sinon null. */
    fun pauseLabel(pauseUntil: String, today: LocalDate): String? {
        val until = runCatching { LocalDate.parse(pauseUntil) }.getOrNull() ?: return null
        return if (!today.isAfter(until)) "en pause jusqu'au ${until.format(dayFmt)} inclus" else null
    }

    fun fmt(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
}
