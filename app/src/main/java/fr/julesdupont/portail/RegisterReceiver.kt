package fr.julesdupont.portail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Redémarrage, mise à jour de l'app ou rafraîchissement périodique → on ré-enregistre la zone. */
class RegisterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Ancienne vérification automatique des mises à jour (jusqu'à la 1.20) : on l'arrête.
        if (intent.action == AppVersion.LEGACY_ACTION_CHECK) {
            AppVersion.cancelLegacyCheck(context)
            return
        }
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) AppVersion.cancelLegacyCheck(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            GeofenceManager.ACTION_REFRESH -> {
                val pending = goAsync()
                GeofenceManager.register(context) { ok, msg ->
                    if (!ok || intent.action != GeofenceManager.ACTION_REFRESH) {
                        val reason = when (intent.action) {
                            Intent.ACTION_BOOT_COMPLETED -> "après le redémarrage"
                            Intent.ACTION_MY_PACKAGE_REPLACED -> "après la mise à jour"
                            else -> "(vérification périodique)"
                        }
                        Prefs(context).log("Zones réactivées $reason : $msg")
                    }
                    pending.finish()
                }
            }
        }
    }
}
