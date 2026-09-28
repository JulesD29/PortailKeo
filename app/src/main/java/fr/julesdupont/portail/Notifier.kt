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
    private const val CHANNEL = "portail"
    private const val ID_INFO = 1
    private const val ID_FALLBACK = 2

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Portail", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Appels automatiques du portail" }
            )
        }
    }

    private fun post(ctx: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!Perms.notifications(ctx)) return
        ensureChannel(ctx)
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
