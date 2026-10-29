package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.TelecomManager

/**
 * Suivi d'un appel automatique, vérifié chaque seconde :
 *  - appel jamais établi → on rappelle (coupure technique, ex. téléphone verrouillé) ;
 *  - appel terminé tout seul peu après avoir commencé → ligne du portail occupée (quelqu'un l'appelle
 *    en même temps) : on rappelle seulement si l'option « Rappeler si le portail est occupé » est cochée ;
 *  - sinon on raccroche après le délai réglé, comme on le fait à la main une fois le portail ouvert.
 */
object CallMonitor {
    /** Fréquence de vérification de l'appel. */
    const val POLL_MS = 1_000L
    /** Au-delà, un appel qui n'a jamais été vu en cours est considéré comme coupé sans sonner. */
    const val CHECK_DELAY_MS = 6_000L
    /**
     * Le portail ne décroche jamais : il laisse sonner jusqu'à ce qu'on raccroche. Un appel qui se termine
     * tout seul avant ce délai a donc échoué (ligne occupée, appel rejeté). Au-delà, il a sonné normalement.
     */
    const val BUSY_LIMIT_MS = 10_000L
    /** Pause avant de rappeler après une coupure technique. */
    const val RETRY_DELAY_MS = 2_000L
    /** Pause avant de rappeler une ligne occupée, le temps que l'autre appel se termine. */
    const val BUSY_RETRY_DELAY_MS = 15_000L
    /** Nombre maximal d'essais, appel initial compris. */
    const val MAX_ATTEMPTS = 3

    enum class Decision {
        /** Rien à faire, on revérifie dans [POLL_MS]. */
        WAIT,
        /** Délai atteint : raccrocher. */
        HANG_UP,
        /** Raccrochage automatique désactivé : l'appel sonne, on arrête de le suivre. */
        RELEASE,
        /** L'appel a sonné puis s'est terminé (raccroché à la main). */
        ENDED,
        /** Coupé sans sonner : nouvel essai rapide. */
        RETRY,
        /** Ligne occupée et option cochée : nouvel essai un peu plus tard. */
        RETRY_BUSY,
        /** Ligne occupée et option décochée : on s'arrête là, sans rappeler. */
        BUSY_STOP,
        /** Plus d'essai possible. */
        GIVE_UP,
    }

    /**
     * Que faire à cette vérification ?
     * @param seenInCall l'appel a déjà été vu en cours depuis qu'il a été lancé
     * @param elapsedMs temps écoulé depuis le lancement de l'appel
     * @param attempt numéro de l'essai en cours (1 = appel initial)
     * @param hangupSeconds raccrochage automatique (0 = jamais)
     * @param retryIfBusy option « Rappeler si le portail est occupé »
     */
    fun decide(
        inCall: Boolean, seenInCall: Boolean, elapsedMs: Long, attempt: Int,
        hangupSeconds: Int, retryIfBusy: Boolean, maxAttempts: Int = MAX_ATTEMPTS,
    ): Decision = when {
        inCall -> when {
            hangupSeconds > 0 && elapsedMs >= hangupSeconds * 1000L -> Decision.HANG_UP
            hangupSeconds <= 0 && elapsedMs >= BUSY_LIMIT_MS -> Decision.RELEASE
            else -> Decision.WAIT
        }
        !seenInCall -> when {
            elapsedMs < CHECK_DELAY_MS -> Decision.WAIT // l'appel n'est peut-être pas encore visible
            attempt < maxAttempts -> Decision.RETRY
            else -> Decision.GIVE_UP
        }
        elapsedMs >= BUSY_LIMIT_MS -> Decision.ENDED
        !retryIfBusy -> Decision.BUSY_STOP
        attempt < maxAttempts -> Decision.RETRY_BUSY
        else -> Decision.GIVE_UP
    }

    // ---------- Accès au téléphone (Android) ----------

    /** Faut-il les autorisations « gérer les appels » pour vérifier et raccrocher. */
    fun canMonitor(ctx: Context) = Perms.callControl(ctx)

    @SuppressLint("MissingPermission")
    private fun telecomInCall(ctx: Context): Boolean =
        runCatching { ctx.getSystemService(TelecomManager::class.java).isInCall }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun telecomEndCall(ctx: Context): Boolean =
        runCatching { ctx.getSystemService(TelecomManager::class.java).endCall() }.getOrDefault(false)

    val defaultInCall: (Context) -> Boolean = { telecomInCall(it) }
    val defaultEndCall: (Context) -> Boolean = { telecomEndCall(it) }

    /** Remplaçables dans les tests (aucun vrai appel). */
    @Volatile var inCallProbe: (Context) -> Boolean = defaultInCall
    @Volatile var endCaller: (Context) -> Boolean = defaultEndCall

    /** Un appel est-il en cours (numérotation, sonnerie ou conversation) ? */
    fun isInCall(ctx: Context): Boolean = inCallProbe(ctx)

    /** Raccroche l'appel en cours. */
    fun endCall(ctx: Context): Boolean = endCaller(ctx)
}
