package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.common.api.ApiException

object GeofenceManager {
    const val GEOFENCE_ID = "portail"
    const val APPROACH_ID = "approche"
    const val ACTION_REFRESH = "fr.julesdupont.portail.REFRESH"

    private fun geofenceIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, GeofenceReceiver::class.java)
            .setAction("fr.julesdupont.portail.GEOFENCE")
        // Google Play Services ajoute l'événement dans l'intent : il doit être MUTABLE (Android 12+).
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(ctx, 0, intent, flags)
    }

    /**
     * (Ré)enregistre la zone selon les réglages. [done] est toujours appelé une fois,
     * avec (succès, message).
     */
    @SuppressLint("MissingPermission")
    fun register(ctx: Context, done: (Boolean, String) -> Unit = { _, _ -> }) {
        val app = ctx.applicationContext
        val p = Prefs(app)
        val client = LocationServices.getGeofencingClient(app)

        if (!p.enabled || !p.hasLocation) {
            client.removeGeofences(geofenceIntent(app))
            cancelRefresh(app)
            done(true, "Automatisation désactivée")
            return
        }
        if (!Perms.fineLocation(app) || !Perms.backgroundLocation(app)) {
            done(false, "Localisation « Toujours autoriser » manquante")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(p.lat, p.lng, p.radius.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        // Pas de déclenchement initial : on ne veut pas appeler juste parce qu'on
        // enregistre les réglages en étant déjà sur place.
        val builder = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
        val twoStage = p.twoStage && p.approachRadius > p.radius
        if (twoStage) {
            builder.addGeofence(
                Geofence.Builder()
                    .setRequestId(APPROACH_ID)
                    .setCircularRegion(p.lat, p.lng, p.approachRadius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            )
        }
        val request = builder.build()

        client.removeGeofences(geofenceIntent(app)).addOnCompleteListener {
            client.addGeofences(request, geofenceIntent(app))
                .addOnSuccessListener {
                    scheduleRefresh(app)
                    done(true, if (twoStage) "Zone active (${p.radius} m, approche ${p.approachRadius} m)"
                               else "Zone active (${p.radius} m)")
                }
                .addOnFailureListener { e ->
                    val code = (e as? ApiException)?.statusCode
                    val msg = if (code != null) GeofenceStatusCodes.getStatusCodeString(code) else e.message
                    p.log("Erreur d'enregistrement de la zone : $msg")
                    done(false, "Erreur : $msg (localisation du téléphone activée ?)")
                }
        }
    }

    /** Ré-enregistre la zone toutes les ~6 h (elle est perdue si on coupe la localisation). */
    private fun scheduleRefresh(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR * 6,
            AlarmManager.INTERVAL_HOUR * 6,
            refreshIntent(ctx)
        )
    }

    private fun cancelRefresh(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(refreshIntent(ctx))
    }

    private fun refreshIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 1,
            Intent(ctx, RegisterReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
