package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager

object CallHelper {
    @SuppressLint("MissingPermission")
    private fun telecomPlace(ctx: Context, uri: Uri) {
        ctx.getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
    }

    /** Passe réellement l'appel. */
    val defaultPlacer: (Context, Uri) -> Unit = { ctx, uri -> telecomPlace(ctx, uri) }

    /** Remplaçable dans les tests pour ne pas passer de vrai appel. */
    @Volatile
    var placer: (Context, Uri) -> Unit = defaultPlacer

    /**
     * Lance l'appel directement (sans passer par le clavier du téléphone).
     * @param automatic true quand l'appel vient de la zone : on mémorise la date pour
     *                  ne pas rappeler le même jour.
     */
    fun call(ctx: Context, number: String, automatic: Boolean): Boolean {
        val prefs = Prefs(ctx)
        if (!Perms.call(ctx)) {
            prefs.log("Échec : permission d'appel manquante")
            Notifier.fallback(ctx, number, "permission manquante")
            return false
        }
        return try {
            placer(ctx, Uri.fromParts("tel", number, null))
            if (automatic) {
                prefs.lastCallDate = AppClock.today().toString()
                prefs.history = History.add(prefs.history, AppClock.now())
            }
            prefs.log(if (automatic) "Appel automatique lancé vers $number" else "Appel manuel lancé vers $number")
            PortalWidget.refresh(ctx)
            true
        } catch (e: Exception) {
            prefs.log("Échec de l'appel : ${e.javaClass.simpleName} ${e.message ?: ""}")
            Notifier.fallback(ctx, number, e.javaClass.simpleName)
            false
        }
    }
}
