package fr.julesdupont.portail

/**
 * Animation de démarrage, mêmes temps que la maquette : le portail s'ouvre (icône animée splash_icon),
 * puis le nom et la phrase apparaissent, puis l'écran s'efface sur l'accueil. Toucher l'écran la passe.
 */
object Splash {
    /** Le nom et la phrase apparaissent en glissant légèrement vers le haut. */
    const val TITLE_DELAY_MS = 1760L
    const val TITLE_MS = 560L
    const val TITLE_SHIFT_DP = 10f
    /** Fondu vers l'accueil. */
    const val FADE_DELAY_MS = 2960L
    const val FADE_MS = 560L
    /** Durée totale avant d'arriver sur l'accueil. */
    const val TOTAL_MS = FADE_DELAY_MS + FADE_MS

    /**
     * Seulement au lancement de l'app (pas après une rotation), si la version la prévoit
     * et si les animations du téléphone ne sont pas désactivées (accessibilité).
     */
    fun showIntro(enabledInBuild: Boolean, coldStart: Boolean, animationsEnabled: Boolean): Boolean =
        enabledInBuild && coldStart && animationsEnabled

    /** « Portail Keo » : « Keo » dans la couleur d'accent, comme le logo complet (CHARTE.md). */
    fun titleWithAccent(name: String, accent: Int): CharSequence {
        val start = name.indexOf("Keo")
        if (start < 0) return name
        return android.text.SpannableString(name).apply {
            setSpan(android.text.style.ForegroundColorSpan(accent), start, start + 3, 0)
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), start, start + 3, 0)
        }
    }
}
