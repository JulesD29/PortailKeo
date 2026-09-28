package fr.julesdupont.portail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Redémarrage, mise à jour de l'app ou rafraîchissement périodique → on ré-enregistre la zone. */
class RegisterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            GeofenceManager.ACTION_REFRESH -> {
                val pending = goAsync()
                GeofenceManager.register(context) { ok, msg ->
                    if (!ok || intent.action != GeofenceManager.ACTION_REFRESH) {
                        Prefs(context).log("Ré-enregistrement (${intent.action?.substringAfterLast('.')}) : $msg")
                    }
                    pending.finish()
                }
            }
        }
    }
}
