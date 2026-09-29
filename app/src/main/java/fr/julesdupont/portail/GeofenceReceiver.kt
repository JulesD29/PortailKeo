package fr.julesdupont.portail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            prefs.log("Erreur zone : ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            if (event.errorCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                Notifier.info(context, "Zone désactivée par le système : ouvrez l'app pour la réactiver.")
            }
            return
        }
        val ids = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        handleTransition(context, event.geofenceTransition, ids)
    }

    companion object {
        /** Logique d'entrée / sortie de zone, séparée de GeofencingEvent pour les tests. */
        fun handleTransition(context: Context, transition: Int, ids: List<String>) {
            val prefs = Prefs(context)
            when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> when {
                    GeofenceManager.GEOFENCE_ID in ids ->
                        AutoCall.attempt(context, "Entrée dans la zone du portail")

                    GeofenceManager.APPROACH_ID in ids -> {
                        val refusal = Rules.check(prefs)
                        if (refusal != null) {
                            prefs.log("Approche détectée, pas de suivi GPS : $refusal")
                        } else {
                            prefs.log("Approche détectée (zone de ${prefs.approachRadius} m)")
                            PortalService.startApproach(context)
                        }
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> if (GeofenceManager.APPROACH_ID in ids) {
                    prefs.log("Sortie de la zone d'approche")
                    PortalService.stop(context)
                }
            }
        }
    }
}
