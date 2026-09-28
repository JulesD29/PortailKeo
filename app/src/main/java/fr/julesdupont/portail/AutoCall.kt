package fr.julesdupont.portail

import android.content.Context

/** Point d'entrée unique d'un appel automatique (zone du portail ou GPS d'approche). */
object AutoCall {
    /** Vérifie les règles puis lance le compte à rebours (ou l'appel direct). */
    fun attempt(ctx: Context, source: String) {
        val prefs = Prefs(ctx)
        val refusal = Rules.check(prefs)
        if (refusal != null) {
            prefs.log("$source, pas d'appel : $refusal")
            PortalService.stop(ctx)
            return
        }
        prefs.log(source)
        if (!PortalService.startCountdown(ctx)) callNow(ctx)
    }

    /** Appel immédiat (fin du compte à rebours, ou si le service n'a pas pu démarrer). */
    fun callNow(ctx: Context) {
        val prefs = Prefs(ctx)
        if (prefs.lastCallDate == java.time.LocalDate.now().toString()) return
        if (CallHelper.call(ctx, prefs.phone, automatic = true)) {
            Notifier.info(ctx, "Appel du portail lancé")
        }
    }
}
