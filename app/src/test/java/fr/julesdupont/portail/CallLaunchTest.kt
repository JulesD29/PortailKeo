package fr.julesdupont.portail

import fr.julesdupont.portail.CallLaunch.Mode
import org.junit.Assert.assertEquals
import org.junit.Test

class CallLaunchTest {
    @Test
    fun `telephone deverrouille appel direct`() =
        assertEquals(Mode.DIRECT, CallLaunch.mode(locked = false, screenOn = true, fullScreenAllowed = true))

    @Test
    fun `telephone verrouille ecran d'appel`() =
        assertEquals(Mode.LOCK_SCREEN, CallLaunch.mode(locked = true, screenOn = true, fullScreenAllowed = true))

    @Test
    fun `ecran eteint ecran d'appel`() =
        assertEquals(Mode.LOCK_SCREEN, CallLaunch.mode(locked = false, screenOn = false, fullScreenAllowed = true))

    @Test
    fun `sans autorisation plein ecran appel direct`() =
        assertEquals(Mode.DIRECT, CallLaunch.mode(locked = true, screenOn = false, fullScreenAllowed = false))
}
