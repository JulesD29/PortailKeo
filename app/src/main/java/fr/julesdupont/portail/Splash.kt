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

    /** Première image de la vidéo (720 × 1280 px) : la tuile du K fait 352 px, centrée à 600 px du haut. */
    const val VIDEO_W_PX = 720f
    const val VIDEO_H_PX = 1280f
    const val TILE_PX = 352f
    const val TILE_CENTER_Y_PX = 600f
    /** Tuile de l'écran de démarrage d'Android (splash_icon_closed) : 56 unités sur 108 d'une icône de 288 dp. */
    const val SPLASH_TILE_DP = 56f * 288f / 108f

    /** dp par pixel de vidéo pour que sa tuile ait exactement la taille de celle de l'écran de démarrage. */
    private const val SCALE = SPLASH_TILE_DP / TILE_PX

    /** Largeur et hauteur de la vidéo à l'écran (dp). */
    fun videoWidthDp(): Float = VIDEO_W_PX * SCALE
    fun videoHeightDp(): Float = VIDEO_H_PX * SCALE

    /** La tuile est un peu au-dessus du milieu de la vidéo : on descend la vidéo d'autant pour la centrer (dp). */
    fun tileOffsetDp(): Float = (VIDEO_H_PX / 2 - TILE_CENTER_Y_PX) * SCALE

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
