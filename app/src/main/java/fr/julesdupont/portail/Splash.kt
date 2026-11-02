package fr.julesdupont.portail

/** Animation de démarrage : le portail s'ouvre (icône animée), puis le nom et la phrase apparaissent. */
object Splash {
    /** Apparition du nom et de la phrase, temps d'affichage, puis fondu vers l'accueil (ms). */
    const val TITLE_MS = 300L
    const val HOLD_MS = 400L
    const val FADE_MS = 250L

    /**
     * Nom et phrase seulement au lancement de l'app (pas après une rotation), si la version les prévoit
     * et si les animations du téléphone ne sont pas désactivées (accessibilité).
     */
    fun showIntro(enabledInBuild: Boolean, coldStart: Boolean, animationsEnabled: Boolean): Boolean =
        enabledInBuild && coldStart && animationsEnabled
}
