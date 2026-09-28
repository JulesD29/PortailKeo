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
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        prefs.log("Entrée dans la zone du portail")
        val refusal = Rules.check(prefs)
        if (refusal != null) {
            prefs.log("Pas d'appel : $refusal")
            return
        }
        if (CallHelper.call(context, prefs.phone, automatic = true)) {
            Notifier.info(context, "Appel du portail lancé")
        }
    }
}
