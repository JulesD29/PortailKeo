package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.TelecomManager

/**
 * Suivi d'un appel automatique : vérifie qu'il sonne bien, rappelle s'il s'est coupé tout de suite,
 * puis raccroche après quelques secondes (comme on le fait à la main une fois le portail ouvert).
 */
object CallMonitor {
    /** Délai avant de vérifier que l'appel est toujours en cours. */
    const val CHECK_DELAY_MS = 6_000L
    /** Pause entre deux essais. */
    const val RETRY_DELAY_MS = 2_000L
    /** Nombre maximal d'essais, appel initial compris. */
    const val MAX_ATTEMPTS = 3

    enum class Decision { RINGING, RETRY, GIVE_UP }

    /** Que faire [CHECK_DELAY_MS] après l'essai n° [attempt] ? */
    fun decide(inCall: Boolean, attempt: Int, maxAttempts: Int = MAX_ATTEMPTS): Decision = when {
        inCall -> Decision.RINGING
        attempt < maxAttempts -> Decision.RETRY
        else -> Decision.GIVE_UP
    }

    /** Délai restant avant de raccrocher, une fois la vérification faite (null = ne pas raccrocher). */
    fun hangupDelayMs(hangupSeconds: Int): Long? =
        if (hangupSeconds <= 0) null else (hangupSeconds * 1000L - CHECK_DELAY_MS).coerceAtLeast(1_000L)

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
