package fr.julesdupont.portail

import java.time.LocalDateTime

object Rules {
    /** Retourne null si l'appel doit être passé, sinon la raison du refus. */
    fun check(p: Prefs, now: LocalDateTime = LocalDateTime.now()): String? {
        if (!p.enabled) return "automatisation désactivée"
        if (p.phone.isBlank()) return "aucun numéro configuré"
        if (now.dayOfWeek.value !in p.days) return "jour non actif"
        val m = now.hour * 60 + now.minute
        val s = p.startMinutes
        val e = p.endMinutes
        val inWindow = if (s <= e) m in s..e else (m >= s || m <= e)
        if (!inWindow) return "hors plage horaire (${fmt(s)}–${fmt(e)})"
        if (p.lastCallDate == now.toLocalDate().toString()) return "déjà appelé aujourd'hui"
        return null
    }

    fun fmt(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
}
