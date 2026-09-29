package fr.julesdupont.portail

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget d'écran d'accueil : état de l'automatisation + bouton « Ouvrir le portail ». */
class PortalWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        /** Met à jour tous les widgets posés (à appeler quand l'état change). */
        fun refresh(ctx: Context) {
            val manager = AppWidgetManager.getInstance(ctx) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(ctx, PortalWidget::class.java))
            if (ids.isEmpty()) return
            val v = views(ctx)
            ids.forEach { manager.updateAppWidget(it, v) }
        }

        /** Titre et ligne d'état affichés par le widget. */
        fun content(ctx: Context): Pair<String, String> {
            val p = Prefs(ctx)
            val permsOk = Perms.fineLocation(ctx) && Perms.backgroundLocation(ctx) && Perms.call(ctx)
            val s = Dashboard.status(p.toRuleConfig(), p.hasLocation && p.phone.isNotBlank(), permsOk, AppClock.now())
            return s.title to s.subtitle.lineSequence().first()
        }

        private fun views(ctx: Context): RemoteViews {
            val (title, subtitle) = content(ctx)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val call = PendingIntent.getActivity(
                ctx, 10, Intent(ctx, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)
            val open = PendingIntent.getActivity(
                ctx, 11, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)
            return RemoteViews(ctx.packageName, R.layout.widget_portal).apply {
                setTextViewText(R.id.widgetTitle, title)
                setTextViewText(R.id.widgetSubtitle, subtitle)
                setOnClickPendingIntent(R.id.widgetCall, call)
                setOnClickPendingIntent(R.id.widgetRoot, open)
            }
        }
    }
}
