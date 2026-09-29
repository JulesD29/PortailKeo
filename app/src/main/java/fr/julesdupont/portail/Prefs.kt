package fr.julesdupont.portail

import android.content.Context
import java.time.format.DateTimeFormatter

/** Réglages de l'app, stockés localement sur le téléphone. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("portail", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    var phone: String
        get() = sp.getString("phone", "") ?: ""
        set(v) = sp.edit().putString("phone", v.trim()).apply()

    val hasLocation: Boolean get() = sp.contains("lat") && sp.contains("lng")

    var lat: Double
        get() = sp.getString("lat", "0")!!.toDouble()
        set(v) = sp.edit().putString("lat", v.toString()).apply()

    var lng: Double
        get() = sp.getString("lng", "0")!!.toDouble()
        set(v) = sp.edit().putString("lng", v.toString()).apply()

    /** Rayon de la zone en mètres. */
    var radius: Int
        get() = sp.getInt("radius", 200)
        set(v) = sp.edit().putInt("radius", v).apply()

    /** Début / fin de la plage horaire, en minutes depuis minuit. */
    var startMinutes: Int
        get() = sp.getInt("start", 7 * 60)
        set(v) = sp.edit().putInt("start", v).apply()

    var endMinutes: Int
        get() = sp.getInt("end", 10 * 60)
        set(v) = sp.edit().putInt("end", v).apply()

    /** Jours actifs : 1 = lundi … 7 = dimanche (java.time.DayOfWeek). */
    var days: Set<Int>
        get() = sp.getStringSet("days", setOf("1", "2", "3", "4", "5"))!!.map { it.toInt() }.toSet()
        set(v) = sp.edit().putStringSet("days", v.map { it.toString() }.toSet()).apply()

    /** Date (yyyy-MM-dd) du dernier appel automatique, pour n'appeler qu'une fois par jour. */
    var lastCallDate: String
        get() = sp.getString("lastCall", "") ?: ""
        set(v) = sp.edit().putString("lastCall", v).apply()

    /** Pause de l'automatisation jusqu'à cette date incluse (yyyy-MM-dd), vide = pas de pause. */
    var pauseUntil: String
        get() = sp.getString("pauseUntil", "") ?: ""
        set(v) = sp.edit().putString("pauseUntil", v).apply()

    /** Ne pas appeler automatiquement les jours fériés. */
    var skipHolidays: Boolean
        get() = sp.getBoolean("skipHolidays", true)
        set(v) = sp.edit().putBoolean("skipHolidays", v).apply()

    /** Détection en deux temps : GPS précis dans la zone d'approche. */
    var twoStage: Boolean
        get() = sp.getBoolean("twoStage", true)
        set(v) = sp.edit().putBoolean("twoStage", v).apply()

    /** Rayon de la zone d'approche en mètres. */
    var approachRadius: Int
        get() = sp.getInt("approachRadius", 2000)
        set(v) = sp.edit().putInt("approachRadius", v).apply()

    /** Secondes de compte à rebours avant l'appel automatique (0 = appel immédiat). */
    var countdownSeconds: Int
        get() = sp.getInt("countdown", 5)
        set(v) = sp.edit().putInt("countdown", v).apply()

    /** Date (yyyy-MM-dd) de la dernière recherche de mise à jour réussie. */
    var lastUpdateCheck: String
        get() = sp.getString("lastUpdateCheck", "") ?: ""
        set(v) = sp.edit().putString("lastUpdateCheck", v).apply()

    val logText: String get() = sp.getString("log", "") ?: ""

    @Synchronized
    fun log(message: String) {
        val stamp = AppClock.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"))
        val lines = ("$stamp  $message\n" + logText).lines().filter { it.isNotBlank() }.take(40)
        sp.edit().putString("log", lines.joinToString("\n")).apply()
    }
}
