package fr.julesdupont.portail

import androidx.appcompat.app.AppCompatDelegate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppThemeTest {
    @After fun tearDown() = AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    @Test
    fun `comme le systeme par defaut`() {
        assertEquals(AppTheme.Mode.SYSTEM, Prefs(TestSupport.app).theme)
        assertEquals(AppTheme.Mode.SYSTEM, AppTheme.parse(null))
        assertEquals(AppTheme.Mode.SYSTEM, AppTheme.parse("inconnu"))
    }

    @Test
    fun `choix enregistre`() {
        Prefs(TestSupport.app).theme = AppTheme.Mode.DARK
        assertEquals(AppTheme.Mode.DARK, Prefs(TestSupport.app).theme)
    }

    @Test
    fun `theme applique a toute l'app`() {
        AppTheme.apply(AppTheme.Mode.DARK)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
        AppTheme.apply(AppTheme.Mode.LIGHT)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
        AppTheme.apply(AppTheme.Mode.SYSTEM)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.getDefaultNightMode())
    }
}
