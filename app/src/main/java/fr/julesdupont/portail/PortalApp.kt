package fr.julesdupont.portail

import android.app.Application
import com.google.android.material.color.DynamicColors

/** Applique les couleurs Material You du fond d'écran (Android 12+), sinon la palette bleue de l'app. */
class PortalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
