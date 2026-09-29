package fr.julesdupont.portail

import java.util.Locale

/** Lecture / écriture des coordonnées « lat, lng » (format copié depuis Google Maps). */
object Coords {
    private val number = Regex("-?\\d+(?:\\.\\d+)?")

    /** Retourne (latitude, longitude), ou null si le texte ne contient pas exactement 2 nombres valides. */
    fun parse(text: String): Pair<Double, Double>? {
        val nums = number.findAll(text).map { it.value.toDouble() }.toList()
        if (nums.size != 2) return null
        val (lat, lng) = nums
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return lat to lng
    }

    fun format(lat: Double, lng: Double): String = String.format(Locale.US, "%.6f, %.6f", lat, lng)
}
