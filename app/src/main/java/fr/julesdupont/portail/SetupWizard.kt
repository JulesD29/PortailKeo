package fr.julesdupont.portail

/** Étapes de l'assistant de premier lancement (logique sans Android, testée à part). */
object SetupWizard {
    enum class Step(val title: String) {
        WELCOME("Bienvenue"),
        PHONE("Numéro du portail"),
        LOCATION("Position du portail"),
        PERMISSIONS("Autorisations"),
        BATTERY("Batterie"),
        TEST("Test et activation"),
    }

    data class State(
        val phoneOk: Boolean,
        val locationOk: Boolean,
        val permissionsOk: Boolean,
        val batteryOk: Boolean,
    )

    val steps = Step.values().toList()

    /** Peut-on passer à l'étape suivante ? (batterie et test sont facultatifs) */
    fun canGoNext(step: Step, s: State): Boolean = when (step) {
        Step.WELCOME -> true
        Step.PHONE -> s.phoneOk
        Step.LOCATION -> s.locationOk
        Step.PERMISSIONS -> s.permissionsOk
        Step.BATTERY -> true
        Step.TEST -> s.phoneOk && s.locationOk && s.permissionsOk
    }

    fun next(step: Step): Step? = steps.getOrNull(step.ordinal + 1)

    fun previous(step: Step): Step? = steps.getOrNull(step.ordinal - 1)

    fun isLast(step: Step) = step == steps.last()

    /** Première étape encore à faire (après un import par QR code, par exemple). */
    fun resumeStep(s: State): Step = when {
        !s.phoneOk -> Step.PHONE
        !s.locationOk -> Step.LOCATION
        !s.permissionsOk -> Step.PERMISSIONS
        !s.batteryOk -> Step.BATTERY
        else -> Step.TEST
    }

    fun progress(step: Step) = "Étape ${step.ordinal + 1}/${steps.size}"
}
