package fr.julesdupont.portail

import android.app.Application

/**
 * Application. Les couleurs sont celles de la charte (CHARTE.md, colors.xml en clair et en sombre) :
 * les couleurs Material You du fond d'écran ne sont volontairement pas appliquées.
 */
class PortalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTheme.apply(Prefs(this).theme) // thème choisi dans Réglages › Apparence
    }
}
