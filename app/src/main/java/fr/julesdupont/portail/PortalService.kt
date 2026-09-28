package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.LocalDate

/**
 * Service de premier plan :
 *  1. approche : GPS précis toutes les 3 s dans la grande zone, jusqu'à l'arrivée au portail ;
 *  2. compte à rebours : notification « Appel dans N s » avec Annuler / Appeler maintenant.
 */
class PortalService : Service() {

    companion object {
        private const val ACTION_APPROACH = "fr.julesdupont.portail.APPROACH"
        private const val ACTION_COUNTDOWN = "fr.julesdupont.portail.COUNTDOWN"
        private const val ACTION_CANCEL = "fr.julesdupont.portail.CANCEL"
        private const val ACTION_CALL_NOW = "fr.julesdupont.portail.CALL_NOW"
        private const val ACTION_STOP = "fr.julesdupont.portail.STOP"
        private const val NOTIF_ID = 10
        private const val APPROACH_TIMEOUT_MS = 20 * 60 * 1000L

        private fun start(ctx: Context, action: String): Boolean = try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, PortalService::class.java).setAction(action))
            true
        } catch (e: Exception) {
            Prefs(ctx).log("Service impossible à démarrer : ${e.javaClass.simpleName}")
            false
        }

        fun startApproach(ctx: Context) = start(ctx, ACTION_APPROACH)

        /** @return false si le compte à rebours est désactivé ou impossible (→ appel direct). */
        fun startCountdown(ctx: Context): Boolean =
            Prefs(ctx).countdownSeconds > 0 && start(ctx, ACTION_COUNTDOWN)

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, PortalService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var client: FusedLocationProviderClient? = null
    private var remaining = 0
    private var inCountdown = false

    private val approachTimeout = Runnable {
        Prefs(this).log("Approche : GPS arrêté après 20 min sans arrivée")
        stopSelf()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (remaining <= 0) {
                AutoCall.callNow(this@PortalService)
                stopSelf()
                return
            }
            show(countdownNotification())
            remaining--
            handler.postDelayed(this, 1000)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onLocation(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_APPROACH -> {
                goForeground(approachNotification(null))
                if (!inCountdown) startLocationUpdates()
            }
            ACTION_COUNTDOWN -> {
                goForeground(approachNotification(null))
                startCountdownInternal()
            }
            ACTION_CANCEL -> {
                handler.removeCallbacks(tick)
                val p = Prefs(this)
                p.lastCallDate = LocalDate.now().toString() // pas de nouvel appel auto aujourd'hui
                p.log("Appel annulé (plus d'appel automatique aujourd'hui)")
                stopSelf()
            }
            ACTION_CALL_NOW -> {
                handler.removeCallbacks(tick)
                AutoCall.callNow(this)
                stopSelf()
            }
            else -> stopSelf() // ACTION_STOP ou redémarrage système
        }
        return START_NOT_STICKY
    }

    private fun goForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIF_ID, n, type)
    }

    private fun show(n: Notification) {
        if (Perms.notifications(this)) {
            try {
                androidx.core.app.NotificationManagerCompat.from(this).notify(NOTIF_ID, n)
            } catch (_: SecurityException) { }
        }
    }

    // ---------- Approche ----------

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (client != null) return
        if (!Perms.fineLocation(this)) { stopSelf(); return }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()
        client = LocationServices.getFusedLocationProviderClient(this).also {
            it.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
        handler.postDelayed(approachTimeout, APPROACH_TIMEOUT_MS)
        Prefs(this).log("Approche : GPS précis activé")
    }

    private fun stopLocationUpdates() {
        client?.removeLocationUpdates(locationCallback)
        client = null
        handler.removeCallbacks(approachTimeout)
    }

    private fun onLocation(loc: Location) {
        if (inCountdown) return
        val p = Prefs(this)
        val d = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, p.lat, p.lng, d)
        val distance = d[0]
        show(approachNotification(distance))
        // On ignore les positions trop imprécises pour éviter un appel prématuré.
        if (distance <= p.radius && loc.accuracy <= maxOf(100f, p.radius.toFloat())) {
            stopLocationUpdates()
            AutoCall.attempt(this, "Arrivée détectée par GPS (${distance.toInt()} m, ±${loc.accuracy.toInt()} m)")
        }
    }

    // ---------- Compte à rebours ----------

    private fun startCountdownInternal() {
        if (inCountdown) return
        inCountdown = true
        stopLocationUpdates()
        remaining = Prefs(this).countdownSeconds
        handler.post(tick)
    }

    // ---------- Notifications ----------

    private fun action(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, PortalService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun approachNotification(distance: Float?): Notification {
        Notifier.ensureChannels(this)
        val text = if (distance == null) "Suivi GPS en cours…"
        else if (distance >= 1000) "Portail à %.1f km".format(distance / 1000) else "Portail à ${distance.toInt()} m"
        return NotificationCompat.Builder(this, Notifier.CHANNEL_APPROACH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Approche du portail")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(0, "Arrêter", action(ACTION_STOP, 20))
            .build()
    }

    private fun countdownNotification(): Notification {
        Notifier.ensureChannels(this)
        return NotificationCompat.Builder(this, Notifier.CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Appel du portail dans $remaining s")
            .setContentText("Touchez Annuler si vous ne voulez pas ouvrir le portail.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Annuler", action(ACTION_CANCEL, 21))
            .addAction(0, "Appeler maintenant", action(ACTION_CALL_NOW, 22))
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopLocationUpdates()
        super.onDestroy()
    }
}
