package fr.julesdupont.portail

/**
 * Ouverture de l'app : l'écran de démarrage d'Android (le pilier du K sur le vert portail) enchaîne sur la vidéo
 * res/raw/ouverture.mp4 (le K se forme, le point part, « Portail Keo · Arrivez, c'est ouvert »), sans son.
 * Toucher l'écran la passe.
 */
object Splash {
    /** Fondu vers l'accueil à la fin de la vidéo. */
    const val FADE_MS = 250L
    /** Au-delà, l'accueil s'affiche même si la vidéo ne s'est pas terminée (vidéo de 5 s). */
    const val MAX_MS = 7_000L

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
