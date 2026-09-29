package fr.julesdupont.portail

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows.shadowOf
import java.time.LocalDateTime

/** Outils communs aux tests Robolectric. */
object TestSupport {
    /** Lundi 5 octobre 2026, 8h00 : jour ouvré, dans la plage horaire par défaut. */
    val MONDAY_8H: LocalDateTime = LocalDateTime.of(2026, 10, 5, 8, 0)
    const val PHONE = "0611223344"

    /** Numéros « appelés » pendant le test (aucun vrai appel). */
    val calls = mutableListOf<String>()

    val app: Application get() = ApplicationProvider.getApplicationContext()

    fun setUp(now: LocalDateTime = MONDAY_8H) {
        AppClock.now = { now }
        calls.clear()
        CallHelper.placer = { _, uri -> calls += uri.schemeSpecificPart }
    }

    fun tearDown() {
        AppClock.reset()
        CallHelper.placer = CallHelper.defaultPlacer
    }

    fun grantCall() = shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE)

    /** Réglages complets et valides, comme après une configuration normale. */
    fun configuredPrefs(countdown: Int = 0): Prefs = Prefs(app).apply {
        enabled = true
        phone = PHONE
        lat = 47.2184
        lng = -1.5536
        radius = 250
        days = setOf(1, 2, 3, 4, 5)
        startMinutes = 7 * 60
        endMinutes = 10 * 60
        countdownSeconds = countdown
    }
}
