package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import java.time.LocalDate

object CallHelper {
    /**
     * Lance l'appel directement (sans passer par le clavier du téléphone).
     * @param automatic true quand l'appel vient de la zone : on mémorise la date pour
     *                  ne pas rappeler le même jour.
     */
    @SuppressLint("MissingPermission")
    fun call(ctx: Context, number: String, automatic: Boolean): Boolean {
        val prefs = Prefs(ctx)
        if (!Perms.call(ctx)) {
            prefs.log("Échec : permission d'appel manquante")
            Notifier.fallback(ctx, number, "permission manquante")
            return false
        }
        return try {
            val tm = ctx.getSystemService(TelecomManager::class.java)
            tm.placeCall(Uri.fromParts("tel", number, null), Bundle())
            if (automatic) prefs.lastCallDate = LocalDate.now().toString()
            prefs.log(if (automatic) "Appel automatique lancé vers $number" else "Appel de test lancé vers $number")
            true
        } catch (e: Exception) {
            prefs.log("Échec de l'appel : ${e.javaClass.simpleName} ${e.message ?: ""}")
            Notifier.fallback(ctx, number, e.javaClass.simpleName)
            false
        }
    }
}
