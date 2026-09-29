package fr.julesdupont.portail

import android.content.Context
import com.google.android.gms.location.Geofence

/**
 * Outils de la version de test (écran « Outils de test ») : simulent les événements
 * de zone sans se déplacer. Ils passent par la même logique que les vraies zones.
 */
object TestTools {
    /** Vrai uniquement dans l'APK « Portail Keo (test) ». */
    val enabled: Boolean get() = BuildConfig.TEST_BUILD

    /** Comme une entrée réelle dans la zone du portail : règles, compte à rebours, appel. */
    fun simulateArrival(ctx: Context) {
        Prefs(ctx).log("[TEST] Arrivée simulée")
        GeofenceReceiver.handleTransition(ctx, Geofence.GEOFENCE_TRANSITION_ENTER, listOf(GeofenceManager.GEOFENCE_ID))
    }

    /** Comme une entrée réelle dans la zone d'approche : démarre le GPS précis. */
    fun simulateApproach(ctx: Context) {
        Prefs(ctx).log("[TEST] Approche simulée")
        GeofenceReceiver.handleTransition(ctx, Geofence.GEOFENCE_TRANSITION_ENTER, listOf(GeofenceManager.APPROACH_ID))
    }

    /** Oublie l'appel automatique du jour, pour pouvoir retester tout de suite. */
    fun resetToday(ctx: Context) {
        val p = Prefs(ctx)
        p.lastCallDate = ""
        p.log("[TEST] Appel du jour réinitialisé")
    }
}
