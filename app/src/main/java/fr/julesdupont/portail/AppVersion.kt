package fr.julesdupont.portail

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Version installée et mises à jour. L'app ne se met plus à jour elle-même (plus d'autorisation d'installer des
 * applis, qui déclenchait les alertes de Play Protect et des antivirus) : Obtainium suit les Releases GitHub et
 * installe les nouvelles versions.
 */
object AppVersion {
    const val RELEASES_URL = "https://github.com/JulesD29/PortailKeo/releases"
    private const val REPO_URL = "https://github.com/JulesD29/PortailKeo"
    const val OBTAINIUM_URL = "https://github.com/ImranR98/Obtainium/releases/latest"

    /** Lien qui ouvre Obtainium sur l'écran d'ajout, avec l'adresse du dépôt déjà remplie. */
    const val OBTAINIUM_ADD = "obtainium://add/$REPO_URL"

    /** Ancienne vérification automatique (jusqu'à la 1.20), à annuler si elle est encore programmée. */
    const val LEGACY_ACTION_CHECK = "fr.julesdupont.portail.UPDATE_CHECK"

    fun name(ctx: Context): String = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"

    /** Annule l'alarme de l'ancienne vérification automatique des mises à jour. */
    fun cancelLegacyCheck(ctx: Context) {
        val app = ctx.applicationContext
        val pi = PendingIntent.getBroadcast(
            app, 2, Intent(app, RegisterReceiver::class.java).setAction(LEGACY_ACTION_CHECK),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        app.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }
}
