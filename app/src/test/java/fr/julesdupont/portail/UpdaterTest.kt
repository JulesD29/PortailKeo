package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class) // org.json n'existe pas sur la JVM seule
class UpdaterTest {

    private fun json(tag: String, name: String = "1.12", assets: String) =
        """{"tag_name":"$tag","name":"$name","body":"Notes de version","assets":[$assets]}"""

    private val apkAsset =
        """{"name":"PortailKeo-1.12.apk","browser_download_url":"https://example.com/PortailKeo-1.12.apk"}"""

    @Test
    fun `lecture d'une release valide`() {
        val r = Updater.parseRelease(json("v12", assets = """{"name":"notes.txt","browser_download_url":"x"},$apkAsset"""))!!
        assertEquals(12L, r.versionCode)
        assertEquals("1.12", r.name)
        assertEquals("https://example.com/PortailKeo-1.12.apk", r.apkUrl)
        assertEquals("Notes de version", r.notes)
    }

    @Test
    fun `release sans APK ignoree`() =
        assertNull(Updater.parseRelease(json("v12", assets = """{"name":"notes.txt","browser_download_url":"x"}""")))

    @Test
    fun `ni tag ni nom numerotes ignore`() =
        assertNull(Updater.parseRelease(json("latest", name = "Version de test", assets = apkAsset)))

    @Test
    fun `pre-release de test numerotee par son nom`() {
        val asset = """{"name":"PortailKeo-test.apk","browser_download_url":"https://example.com/PortailKeo-test.apk"}"""
        val r = Updater.parseRelease(json("test", name = "Test 1.15", assets = asset))!!
        assertEquals(15L, r.versionCode)
        assertEquals("https://example.com/PortailKeo-test.apk", r.apkUrl)
    }

    @Test
    fun `le numero du tag est prioritaire sur le nom`() =
        assertEquals(12L, Updater.parseRelease(json("v12", name = "1.99", assets = apkAsset))!!.versionCode)

    @Test
    fun `nom manquant remplace par le tag`() =
        assertEquals("v12", Updater.parseRelease(json("v12", name = "", assets = apkAsset))!!.name)

    @Test
    fun `notes affichees sans titres markdown`() {
        assertEquals("- Nouvelle interface\n- Widget", Updater.displayNotes("## Nouveautés\n- Nouvelle interface\n- Widget\n"))
        assertEquals("Une nouvelle version est disponible.", Updater.displayNotes("## Nouveautés\n"))
    }

    @Test
    fun `comparaison des versions`() {
        val r = Updater.Release(12, "1.12", "u", "")
        assertTrue(Updater.isNewer(r, 11))
        assertFalse(Updater.isNewer(r, 12))
        assertFalse(Updater.isNewer(r, 13))
    }

    @Test
    fun `verification automatique au plus une fois par heure`() {
        val h = Updater.MIN_INTERVAL_MS
        assertTrue("jamais vérifié", Updater.shouldCheck(0L, 10 * h, interactive = false))
        assertFalse("vérifié il y a 10 min", Updater.shouldCheck(10 * h, 10 * h + h / 6, interactive = false))
        assertTrue("vérifié il y a 1 h", Updater.shouldCheck(10 * h, 11 * h, interactive = false))
        assertTrue("sur demande : toujours", Updater.shouldCheck(10 * h, 10 * h + 1, interactive = true))
        assertTrue("horloge reculée", Updater.shouldCheck(10 * h, 9 * h, interactive = false))
    }

    @Test
    fun `plus tard ne repropose pas la meme version`() {
        val r = Updater.Release(21, "1.21", "u", "")
        assertTrue(Updater.shouldOffer(r, dismissedVersionCode = 0))
        assertFalse(Updater.shouldOffer(r, dismissedVersionCode = 21))
        assertTrue("version suivante", Updater.shouldOffer(Updater.Release(22, "1.22", "u", ""), 21))
    }

    @Test
    fun `l'app ne demande pas a installer des applis`() {
        val app = TestSupport.app
        val perms = app.packageManager.getPackageInfo(app.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        assertFalse("REQUEST_INSTALL_PACKAGES déclenche les alertes de Play Protect",
            "android.permission.REQUEST_INSTALL_PACKAGES" in perms)
    }
}
