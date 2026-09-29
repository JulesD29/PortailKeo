package fr.julesdupont.portail

import java.time.LocalDate
import java.time.LocalDateTime

/** Horloge de l'app. Les tests la remplacent pour se placer à une date/heure précise. */
object AppClock {
    @Volatile
    var now: () -> LocalDateTime = { LocalDateTime.now() }

    fun today(): LocalDate = now().toLocalDate()

    fun reset() {
        now = { LocalDateTime.now() }
    }
}
