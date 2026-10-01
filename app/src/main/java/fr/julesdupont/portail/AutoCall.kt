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

    /**
     * Appel immédiat (fin du compte à rebours, ou si le service n'a pas pu démarrer),
     * puis suivi de l'appel : nouvel essai s'il se coupe sans sonner, raccrochage automatique.
     * @return true si l'appel a été lancé.
     */
    fun callNow(ctx: Context): Boolean {
        val prefs = Prefs(ctx)
        if (prefs.lastCallDate == AppClock.today().toString()) return false
        val ok = CallHelper.call(ctx, prefs.phone, automatic = true)
        if (ok) PortalService.startMonitor(ctx)
        return ok
    }
}
