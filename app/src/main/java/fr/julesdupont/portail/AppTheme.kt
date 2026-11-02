package fr.julesdupont.portail

import androidx.appcompat.app.AppCompatDelegate

/** Thème de l'app choisi dans Réglages › Apparence : clair, sombre ou comme le système (par défaut). */
object AppTheme {
    enum class Mode(val label: String, val nightMode: Int) {
        SYSTEM("Comme le système", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        LIGHT("Clair", AppCompatDelegate.MODE_NIGHT_NO),
        DARK("Sombre", AppCompatDelegate.MODE_NIGHT_YES),
    }

    /** Valeur enregistrée → mode ; inconnue ou vide → comme le système. */
    fun parse(value: String?): Mode = Mode.entries.firstOrNull { it.name == value } ?: Mode.SYSTEM

    /** Applique le thème à toute l'app (les écrans ouverts sont recréés si besoin). */
    fun apply(mode: Mode) {
        if (AppCompatDelegate.getDefaultNightMode() != mode.nightMode) AppCompatDelegate.setDefaultNightMode(mode.nightMode)
    }
}
