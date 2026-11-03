package fr.julesdupont.portail

import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppVersionTest {
    private val app get() = TestSupport.app

    @Test
    fun `l'app ne demande plus a installer des applis`() {
        val perms = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        assertFalse("REQUEST_INSTALL_PACKAGES déclenche les alertes", "android.permission.REQUEST_INSTALL_PACKAGES" in perms)
    }

    @Test
    fun `lien Obtainium vers le depot`() {
        assertEquals("obtainium://add/https://github.com/JulesD29/PortailKeo", AppVersion.OBTAINIUM_ADD)
        assertTrue(AppVersion.RELEASES_URL.startsWith("https://github.com/JulesD29/PortailKeo"))
    }

    @Test
    fun `version installee lisible et ancienne verification annulable sans plantage`() {
        assertTrue(AppVersion.name(app).isNotBlank())
        AppVersion.cancelLegacyCheck(app)
    }
}
