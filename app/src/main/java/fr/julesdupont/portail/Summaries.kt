package fr.julesdupont.portail

/** Résumés d'une ligne affichés sous chaque entrée de l'onglet Réglages (logique pure). */
object Summaries {
    private val dayNames = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")

    /** « Tous les jours », « Lun → ven », « Lun, mer, ven ». */
    fun days(days: Set<Int>): String {
        val d = days.filter { it in 1..7 }.sorted()
        val text = when {
            d.isEmpty() -> "aucun jour"
            d.size == 7 -> "tous les jours"
            d.size >= 3 && d.last() - d.first() == d.size - 1 -> "${dayNames[d.first() - 1]} → ${dayNames[d.last() - 1]}"
            else -> d.joinToString(", ") { dayNames[it - 1] }
        }
        return text.replaceFirstChar { it.uppercase() }
    }

    fun schedule(days: Set<Int>, startMinutes: Int, endMinutes: Int): String =
        "${days(days)} · ${Rules.fmt(startMinutes)}–${Rules.fmt(endMinutes)}"

    fun phone(phone: String): String = phone.ifBlank { "À renseigner" }

    fun zone(hasLocation: Boolean, radius: Int, twoStage: Boolean, approach: Int): String =
        if (!hasLocation) "À placer sur la carte"
        else "Zone du portail ${Zones.label(radius)}" + if (twoStage) " · approche ${Zones.label(approach)}" else ""

    fun call(countdownSeconds: Int, hangupSeconds: Int): String =
        (if (countdownSeconds <= 0) "Appel immédiat" else "Compte à rebours $countdownSeconds s") + " · " +
            (if (hangupSeconds <= 0) "pas de raccrochage automatique" else "raccroche après $hangupSeconds s")

    fun feedback(vibrate: Boolean, voice: Feedback.Voice): String {
        val parts = buildList {
            if (vibrate) add("vibrations")
            when (voice) {
                Feedback.Voice.ALWAYS -> add("annonce vocale")
                Feedback.Voice.HEADSET_ONLY -> add("annonce avec écouteurs")
                Feedback.Voice.OFF -> {}
            }
        }
        return parts.joinToString(" · ").ifEmpty { "désactivé" }.replaceFirstChar { it.uppercase() }
    }

    fun shortcuts(voiceEnabled: Boolean, spokenName: String): String =
        if (voiceEnabled) "« Ok Google, ouvre $spokenName »" else "Tuile et widget · commande vocale désactivée"

    fun permissions(missing: Int): String = when (missing) {
        0 -> "Tout est accordé"
        1 -> "1 réglage à faire"
        else -> "$missing réglages à faire"
    }
}
