package fr.julesdupont.portail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/** Résultat de l'installation d'une mise à jour. */
class InstallReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "fr.julesdupont.portail.INSTALL_STATUS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Android demande confirmation à l'utilisateur : on affiche son écran.
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { context.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> prefs.log("Mise à jour installée")
            PackageInstaller.STATUS_FAILURE_ABORTED -> prefs.log("Mise à jour annulée")
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                prefs.log("Mise à jour refusée : signature différente. Désinstallez puis installez l'APK à la main.")
                Notifier.info(context, "Mise à jour impossible : désinstallez l'app puis installez le nouvel APK.")
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "erreur inconnue"
                prefs.log("Échec de l'installation : $msg")
                Notifier.info(context, "Échec de la mise à jour : $msg")
            }
        }
    }
}
