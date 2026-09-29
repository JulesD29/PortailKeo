package fr.julesdupont.portail

import kotlin.math.ceil
import kotlin.math.roundToInt

/** Règles de taille des zones, partagées par l'écran de réglages et la carte. */
object Zones {
    const val RADIUS_MIN = 50
    const val RADIUS_MAX = 1000
    const val RADIUS_STEP = 10
    const val APPROACH_MIN = 500
    const val APPROACH_MAX = 5000
    const val APPROACH_STEP = 100
    /** Écart minimal entre la zone du portail et la zone d'approche. */
    const val MIN_GAP = 300

    fun minApproach(radius: Int) = radius + MIN_GAP

    /** Message d'erreur si les rayons sont invalides, sinon null. */
    fun validationError(radius: Int?, approach: Int?, twoStage: Boolean): String? {
        if (radius == null || radius < RADIUS_MIN || radius > RADIUS_MAX) {
            return "Rayon entre $RADIUS_MIN et $RADIUS_MAX m"
        }
        if (twoStage && (approach == null || approach < minApproach(radius) || approach > APPROACH_MAX)) {
            return "Zone d'approche : au moins ${minApproach(radius)} m et au plus $APPROACH_MAX m"
        }
        return null
    }

    /** Arrondit au pas le plus proche et reste dans [from, to] (exigé par les curseurs). */
    fun snap(value: Int, from: Int, to: Int, step: Int): Int {
        val snapped = from + ((value - from).toDouble() / step).roundToInt() * step
        return snapped.coerceIn(from, to)
    }

    fun snapRadius(value: Int) = snap(value, RADIUS_MIN, RADIUS_MAX, RADIUS_STEP)

    /** Zone d'approche ramenée sur le curseur, et toujours au moins [minApproach] du rayon. */
    fun snapApproach(value: Int, radius: Int): Int {
        val min = minApproach(radius)
        val atLeast = if (value < min) (ceil(min.toDouble() / APPROACH_STEP) * APPROACH_STEP).toInt() else value
        return snap(atLeast, APPROACH_MIN, APPROACH_MAX, APPROACH_STEP).let {
            if (it < min) it + APPROACH_STEP else it
        }.coerceAtMost(APPROACH_MAX)
    }

    /** "250 m" ou "2,5 km". */
    fun label(meters: Int): String =
        if (meters < 1000) "$meters m" else String.format(java.util.Locale.FRANCE, "%.1f km", meters / 1000.0).replace(",0 km", " km")
}
