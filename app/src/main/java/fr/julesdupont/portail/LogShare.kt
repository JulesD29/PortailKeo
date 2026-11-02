package fr.julesdupont.portail

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Texte du journal à partager (onglet Historique › Partager) : contexte utile pour comprendre un problème
 * (version, téléphone, réglages, autorisations, derniers appels) puis le journal complet.
 * Les numéros de téléphone sont masqués et la position du portail n'est jamais incluse.
 */
object LogShare {
    data class Info(
        val versionName: String,
        val testBuild: Boolean,
        val device: String,
        val androidVersion: String,
        val now: LocalDateTime,
        val statusTitle: String,
        val enabled: Boolean,
        val days: Set<Int>,
        val startMinutes: Int,
        val endMinutes: Int,
        val skipHolidays: Boolean,
        val hasLocation: Boolean,
        val radius: Int,
        val twoStage: Boolean,
        val approachRadius: Int,
        val countdownSeconds: Int,
        val hangupSeconds: Int,
        val retryIfBusy: Boolean,
        /** Autorisations manquantes, une par élément (vide = tout est accordé). */
        val missingPermissions: List<String>,
        val calls: List<LocalDateTime>,
        val log: String,
    )

    /** Numéro de téléphone : 8 chiffres ou plus, éventuellement séparés par des espaces, points ou tirets. */
    private val phoneLike = Regex("\\+?\\d(?:[ .-]?\\d){7,}")

    /** « 0253591329 » → « 02••••••29 » : seuls les 2 premiers et les 2 derniers chiffres restent. */
    fun maskPhone(number: String): String {
        val total = number.count { it.isDigit() }
        var seen = 0
        return buildString {
            for (ch in number) {
                if (ch.isDigit()) {
                    append(if (seen < 2 || seen >= total - 2) ch else '•')
                    seen++
                } else append(ch)
            }
        }
    }

    /** Masque tous les numéros de téléphone d'un texte. */
    fun maskPhones(text: String): String = phoneLike.replace(text) { maskPhone(it.value) }

    fun text(i: Info): String = buildString {
        val stamp = i.now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        appendLine("Journal Portail Keo ${i.versionName}${if (i.testBuild) " (version de test)" else ""} — $stamp")
        appendLine("Téléphone : ${i.device} · Android ${i.androidVersion}")
        appendLine("État : ${i.statusTitle} · appel automatique ${if (i.enabled) "activé" else "désactivé"}")
        appendLine("Horaires : ${Summaries.schedule(i.days, i.startMinutes, i.endMinutes)}" +
            if (i.skipHolidays) " · pas d'appel les jours fériés" else "")
        appendLine("Zones : " + if (!i.hasLocation) "position du portail non renseignée"
            else "portail ${Zones.label(i.radius)}" +
                if (i.twoStage) " · approche ${Zones.label(i.approachRadius)} · GPS précis à l'approche" else " · sans GPS précis")
        appendLine("Appel : ${Summaries.call(i.countdownSeconds, i.hangupSeconds)} · rappel si occupé : ${if (i.retryIfBusy) "oui" else "non"}")
        appendLine("Autorisations : " + if (i.missingPermissions.isEmpty()) "tout est accordé"
            else "manquent " + i.missingPermissions.joinToString(", "))
        appendLine("Derniers appels : " + History.recent(i.calls, 5).joinToString(", ").ifEmpty { "aucun" })
        appendLine()
        append(maskPhones(i.log.lines().filter { it.isNotBlank() }.joinToString("\n")).ifEmpty { "(journal vide)" })
    }
}
