package fr.julesdupont.portail

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Rules {
    private val dayFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Retourne null si l'appel doit être passé, sinon la raison du refus. */
    fun check(p: Prefs, now: LocalDateTime = LocalDateTime.now()): String? {
        if (!p.enabled) return "automatisation désactivée"
        if (p.phone.isBlank()) return "aucun numéro configuré"
        pauseLabel(p, now.toLocalDate())?.let { return it }
        if (p.skipHolidays) Holidays.name(now.toLocalDate())?.let { return "jour férié ($it)" }
        if (now.dayOfWeek.value !in p.days) return "jour non actif"
        val m = now.hour * 60 + now.minute
        val s = p.startMinutes
        val e = p.endMinutes
        val inWindow = if (s <= e) m in s..e else (m >= s || m <= e)
        if (!inWindow) return "hors plage horaire (${fmt(s)}–${fmt(e)})"
        if (p.lastCallDate == now.toLocalDate().toString()) return "déjà appelé aujourd'hui"
        return null
    }

    /** "en pause jusqu'au …" si une pause est en cours, sinon null. */
    fun pauseLabel(p: Prefs, today: LocalDate = LocalDate.now()): String? {
        val until = runCatching { LocalDate.parse(p.pauseUntil) }.getOrNull() ?: return null
        return if (!today.isAfter(until)) "en pause jusqu'au ${until.format(dayFmt)} inclus" else null
    }

    fun fmt(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
}
