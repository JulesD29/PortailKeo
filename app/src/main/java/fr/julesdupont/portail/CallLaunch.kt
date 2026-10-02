package fr.julesdupont.portail

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * Comment lancer l'appel automatique : directement (téléphone déverrouillé) ou, si le téléphone
 * est verrouillé, depuis un écran affiché par-dessus le verrouillage (comme un appel entrant),
 * pour que l'appel parte au premier plan, comme quand on touche « Ouvrir le portail ».
 */
object CallLaunch {
    enum class Mode { DIRECT, LOCK_SCREEN }

    /** Délai après lequel l'app appelle quand même directement si l'écran d'appel ne s'est pas affiché. */
    const val FALLBACK_DELAY_MS = 5_000L

    fun mode(locked: Boolean, screenOn: Boolean, fullScreenAllowed: Boolean): Mode =
        if ((locked || !screenOn) && fullScreenAllowed) Mode.LOCK_SCREEN else Mode.DIRECT

    // ---------- État du téléphone (remplaçable dans les tests) ----------

    val defaultLocked: (Context) -> Boolean = {
        it.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked ?: false
    }
    val defaultScreenOn: (Context) -> Boolean = {
        it.getSystemService(PowerManager::class.java)?.isInteractive ?: true
    }
    val defaultFullScreen: (Context) -> Boolean = {
        if (Build.VERSION.SDK_INT >= 34) it.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        else true
    }

    @Volatile var lockedProbe = defaultLocked
    @Volatile var screenOnProbe = defaultScreenOn
    @Volatile var fullScreenProbe = defaultFullScreen

    fun current(ctx: Context): Mode = mode(lockedProbe(ctx), screenOnProbe(ctx), fullScreenProbe(ctx))

    fun reset() {
        lockedProbe = defaultLocked
        screenOnProbe = defaultScreenOn
        fullScreenProbe = defaultFullScreen
    }
}
