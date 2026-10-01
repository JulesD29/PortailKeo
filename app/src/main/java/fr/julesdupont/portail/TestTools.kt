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

    /**
     * Arrivée simulée avec un compte à rebours de [seconds] s : le temps de verrouiller le téléphone,
     * pour reproduire un appel automatique téléphone verrouillé.
     */
    fun simulateArrivalDelayed(ctx: Context, seconds: Int = 20) {
        val p = Prefs(ctx)
        p.log("[TEST] Arrivée simulée dans $seconds s : verrouillez le téléphone")
        AutoCall.attempt(ctx, "[TEST] Arrivée simulée (différée)", countdownSeconds = seconds)
    }

    /** Comme une entrée réelle dans la zone d'approche : démarre le GPS précis. */
    fun simulateApproach(ctx: Context) {
        Prefs(ctx).log("[TEST] Approche simulée")
        GeofenceReceiver.handleTransition(ctx, Geofence.GEOFENCE_TRANSITION_ENTER, listOf(GeofenceManager.APPROACH_ID))
    }

    /** Efface tous les réglages : l'app redémarre comme au premier lancement (assistant). */
    fun resetSetup(ctx: Context) {
        ctx.getSharedPreferences("portail", Context.MODE_PRIVATE).edit().clear().commit()
    }

    /** Fait réapparaître l'écran « Quoi de neuf ». */
    fun resetWhatsNew(ctx: Context) {
        Prefs(ctx).lastSeenWhatsNew = 0
    }

    /** Oublie l'appel automatique du jour, pour pouvoir retester tout de suite. */
    fun resetToday(ctx: Context) {
        val p = Prefs(ctx)
        p.lastCallDate = ""
        p.log("[TEST] Appel du jour réinitialisé")
    }
}
