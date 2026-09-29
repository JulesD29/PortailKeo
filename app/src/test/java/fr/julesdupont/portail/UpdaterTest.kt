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
    fun `tag sans numero ignore`() = assertNull(Updater.parseRelease(json("latest", assets = apkAsset)))

    @Test
    fun `nom manquant remplace par le tag`() =
        assertEquals("v12", Updater.parseRelease(json("v12", name = "", assets = apkAsset))!!.name)

    @Test
    fun `comparaison des versions`() {
        val r = Updater.Release(12, "1.12", "u", "")
        assertTrue(Updater.isNewer(r, 11))
        assertFalse(Updater.isNewer(r, 12))
        assertFalse(Updater.isNewer(r, 13))
    }
}
