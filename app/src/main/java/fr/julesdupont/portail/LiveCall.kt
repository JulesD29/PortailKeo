package fr.julesdupont.portail

/**
 * Compte à rebours ou appel automatique en cours, tenu à jour par [PortalService]
 * et affiché en direct sur l'écran d'accueil (même processus).
 */
object LiveCall {
    sealed interface State {
        data object Idle : State
        /** Compte à rebours avant l'appel ; 0 = l'appel part. */
        data class Countdown(val remaining: Int, val total: Int) : State
        /** Appel suivi : [elapsedMs] depuis le début de l'essai [attempt]. */
        data class Calling(val elapsedMs: Long, val hangupSeconds: Int, val attempt: Int) : State
        /** Nouvel essai prévu (appel coupé sans sonner, ou ligne occupée). */
        data class Retrying(val nextAttempt: Int, val busy: Boolean) : State
        /** Appel terminé (raccroché), le service s'arrête bientôt. */
        data object Done : State
    }

    @Volatile var state: State = State.Idle

    /** Ce que l'écran d'accueil affiche. */
    data class Display(
        val badge: String,
        val title: String,
        val subtitle: String,
        /** Grand chiffre au centre de l'anneau. */
        val big: String,
        val small: String,
        /** Avancement de l'anneau, 0 à 100 ; null = indéterminé. */
        val progress: Int?,
        /** Étapes franchies : 1 = arrivée, 2 = appel, 3 = raccrochage. */
        val steps: Int,
        val action: Action,
    )

    enum class Action { CANCEL, HANG_UP, NONE }

    /** null quand il n'y a rien à afficher (état normal de l'accueil). */
    fun display(s: State, maxAttempts: Int = CallMonitor.MAX_ATTEMPTS): Display? = when (s) {
        State.Idle -> null
        is State.Countdown ->
            if (s.remaining > 0) Display(
                badge = "Arrivée détectée",
                title = "Appel du portail dans ${s.remaining} s",
                subtitle = "Touchez Annuler si vous ne voulez pas ouvrir le portail",
                big = "${s.remaining} s", small = "avant l'appel",
                progress = if (s.total > 0) 100 * (s.total - s.remaining) / s.total else null,
                steps = 1, action = Action.CANCEL,
            ) else Display(
                badge = "Arrivée détectée", title = "Appel du portail…", subtitle = "L'appel part",
                big = "…", small = "", progress = null, steps = 1, action = Action.NONE,
            )
        is State.Calling -> {
            val seconds = (s.elapsedMs / 1000).toInt()
            val badge = "Appel en cours · essai ${s.attempt}/$maxAttempts"
            if (s.hangupSeconds > 0) Display(
                badge = badge, title = "Le portail sonne",
                subtitle = "Raccrochage automatique dans ${(s.hangupSeconds - seconds).coerceAtLeast(0)} s",
                big = "$seconds s", small = "sur ${s.hangupSeconds} s",
                progress = (100 * seconds / s.hangupSeconds).coerceIn(0, 100),
                steps = 2, action = Action.HANG_UP,
            ) else Display(
                badge = badge, title = "Le portail sonne", subtitle = "Pas de raccrochage automatique",
                big = "$seconds s", small = "", progress = null, steps = 2, action = Action.HANG_UP,
            )
        }
        is State.Retrying -> Display(
            badge = "Nouvel essai ${s.nextAttempt}/$maxAttempts",
            title = if (s.busy) "Ligne du portail occupée" else "L'appel s'est coupé",
            subtitle = if (s.busy) "L'app rappelle dans ${CallMonitor.BUSY_RETRY_DELAY_MS / 1000} s" else "L'app rappelle tout de suite",
            big = "…", small = "", progress = null, steps = 1, action = Action.HANG_UP,
        )
        State.Done -> Display(
            badge = "Terminé", title = "Portail appelé", subtitle = "Appel terminé",
            big = "✓", small = "", progress = 100, steps = 3, action = Action.NONE,
        )
    }
}
