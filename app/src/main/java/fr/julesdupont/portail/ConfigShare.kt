package fr.julesdupont.portail

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale

/**
 * Configuration partagée par QR code, sous forme de lien :
 * portailkeo://config?v=1&n=0611…&lat=…&lng=…&r=250&a=2000&ts=1&d=12345&s=420&e=600&c=5&h=1
 * Ne contient que les réglages utiles à un collègue (pas la pause, ni le journal, ni l'activation).
 */
data class SharedConfig(
    val phone: String,
    val lat: Double,
    val lng: Double,
    val radius: Int,
    val approach: Int = 2000,
    val twoStage: Boolean = true,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5),
    val startMinutes: Int = 7 * 60,
    val endMinutes: Int = 10 * 60,
    val countdown: Int = 5,
    val skipHolidays: Boolean = true,
)

object ConfigShare {
    const val SCHEME = "portailkeo"
    const val TEST_SCHEME = "portailkeo-test"
    private const val HOST = "config"
    private const val VERSION = "1"
    private val phonePattern = Regex("^\\+?[0-9 .]{3,20}$")

    fun encode(c: SharedConfig, scheme: String = SCHEME): String {
        fun e(v: String) = URLEncoder.encode(v, "UTF-8")
        val params = listOf(
            "v" to VERSION,
            "n" to e(c.phone),
            "lat" to String.format(Locale.US, "%.6f", c.lat),
            "lng" to String.format(Locale.US, "%.6f", c.lng),
            "r" to c.radius.toString(),
            "a" to c.approach.toString(),
            "ts" to if (c.twoStage) "1" else "0",
            "d" to c.days.sorted().joinToString(""),
            "s" to c.startMinutes.toString(),
            "e" to c.endMinutes.toString(),
            "c" to c.countdown.toString(),
            "h" to if (c.skipHolidays) "1" else "0",
        )
        return "$scheme://$HOST?" + params.joinToString("&") { (k, v) -> "$k=$v" }
    }

    /** Lit un texte scanné. Null si ce n'est pas une configuration Portail Keo valide. */
    fun decode(text: String): SharedConfig? {
        val uri = runCatching { URI(text.trim()) }.getOrNull() ?: return null
        if (uri.scheme !in setOf(SCHEME, TEST_SCHEME) || uri.host != HOST) return null
        val q = (uri.rawQuery ?: return null).split("&").mapNotNull {
            val i = it.indexOf('=')
            if (i <= 0) null else it.substring(0, i) to URLDecoder.decode(it.substring(i + 1), "UTF-8")
        }.toMap()
        if (q["v"] != VERSION) return null

        val phone = q["n"]?.trim().orEmpty()
        if (!phonePattern.matches(phone)) return null
        val lat = q["lat"]?.toDoubleOrNull() ?: return null
        val lng = q["lng"]?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        val radius = q["r"]?.toIntOrNull() ?: return null
        val twoStage = q["ts"] != "0"
        val approach = q["a"]?.toIntOrNull() ?: Zones.snapApproach(2000, radius)
        if (Zones.validationError(radius, approach, twoStage) != null) return null
        val days = (q["d"] ?: "12345").map { it.digitToIntOrNull() ?: return null }.toSet()
        if (days.isEmpty() || days.any { it !in 1..7 }) return null
        val start = q["s"]?.toIntOrNull() ?: (7 * 60)
        val end = q["e"]?.toIntOrNull() ?: (10 * 60)
        if (start !in 0..1439 || end !in 0..1439) return null
        val countdown = q["c"]?.toIntOrNull() ?: 5
        if (countdown !in 0..60) return null

        return SharedConfig(phone, lat, lng, radius, approach, twoStage, days, start, end, countdown, q["h"] != "0")
    }

    fun fromPrefs(p: Prefs) = SharedConfig(
        phone = p.phone, lat = p.lat, lng = p.lng, radius = p.radius, approach = p.approachRadius,
        twoStage = p.twoStage, days = p.days, startMinutes = p.startMinutes, endMinutes = p.endMinutes,
        countdown = p.countdownSeconds, skipHolidays = p.skipHolidays,
    )

    /** Applique une configuration importée. Ne touche ni à l'activation, ni à la pause, ni au journal. */
    fun apply(p: Prefs, c: SharedConfig) {
        p.phone = c.phone
        p.lat = c.lat
        p.lng = c.lng
        p.radius = c.radius
        p.approachRadius = c.approach
        p.twoStage = c.twoStage
        p.days = c.days
        p.startMinutes = c.startMinutes
        p.endMinutes = c.endMinutes
        p.countdownSeconds = c.countdown
        p.skipHolidays = c.skipHolidays
        p.log("Configuration importée (portail ${c.phone})")
    }

    /** Résumé lisible pour la fenêtre de confirmation. */
    fun summary(c: SharedConfig): String {
        val dayNames = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")
        return buildString {
            appendLine("Numéro du portail : ${c.phone}")
            appendLine("Position : ${Coords.format(c.lat, c.lng)}")
            appendLine("Zone du portail : ${Zones.label(c.radius)}")
            appendLine(if (c.twoStage) "Zone d'approche : ${Zones.label(c.approach)}" else "GPS précis à l'approche : non")
            appendLine("Jours : ${c.days.sorted().joinToString(", ") { dayNames[it - 1] }}")
            appendLine("Horaires : ${Rules.fmt(c.startMinutes)}–${Rules.fmt(c.endMinutes)}")
            appendLine("Compte à rebours : ${c.countdown} s")
            append("Jours fériés : ${if (c.skipHolidays) "pas d'appel" else "appel"}")
        }
    }
}
