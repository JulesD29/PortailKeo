package fr.julesdupont.portail

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    const val CHANNEL = "portail"
    const val CHANNEL_APPROACH = "approche"
    private const val ID_INFO = 1
    private const val ID_FALLBACK = 2
    private const val ID_UPDATE = 3
    const val ID_LOCKED_CALL = 4
    const val EXTRA_SHOW_UPDATE = "show_update"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Portail", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Appels automatiques du portail" }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_APPROACH) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_APPROACH, "Approche", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Suivi GPS à l'approche du portail" }
            )
        }
    }

    private fun post(ctx: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!Perms.notifications(ctx)) return
        ensureChannels(ctx)
        try {
            NotificationManagerCompat.from(ctx).notify(id, builder.build())
        } catch (_: SecurityException) { }
    }

    private fun openApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun info(ctx: Context, text: String) {
        post(ctx, ID_INFO, NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true))
    }

    /** Appel automatique téléphone verrouillé : affiche l'écran d'appel par-dessus le verrouillage. */
    fun lockedCall(ctx: Context) {
        val pi = PendingIntent.getActivity(
            ctx, 4, CallActivity.autoIntent(ctx),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        post(ctx, ID_LOCKED_CALL, NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Appel du portail")
            .setContentText("Ouverture du portail…")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000))
    }

    fun cancelLockedCall(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(ID_LOCKED_CALL)
    }

    fun update(ctx: Context, version: String) {
        val pi = PendingIntent.getActivity(
            ctx, 3,
            Intent(ctx, MainActivity::class.java).putExtra(EXTRA_SHOW_UPDATE, true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        post(ctx, ID_UPDATE, NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Mise à jour disponible")
            .setContentText("${ctx.getString(R.string.app_name)} $version : touchez pour installer")
            .setContentIntent(pi)
            .setAutoCancel(true))
    }

    /** Si l'appel automatique échoue, propose un bouton "Appeler" dans une notification. */
    fun fallback(ctx: Context, number: String, reason: String) {
        val callIntent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            ctx, 1, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        post(ctx, ID_FALLBACK, NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Ouvrir le portail ?")
            .setContentText("Appel automatique impossible ($reason). Touchez pour appeler.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pi)
            .addAction(0, "Appeler", pi)
            .setAutoCancel(true))
    }
}
