package fr.julesdupont.portail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZonesTest {

    @Test
    fun `rayons valides acceptes`() {
        assertNull(Zones.validationError(250, 2000, twoStage = true))
        assertNull(Zones.validationError(50, 350, twoStage = true))
        assertNull(Zones.validationError(1000, 5000, twoStage = true))
    }

    @Test
    fun `rayon du portail hors limites`() {
        assertTrue(Zones.validationError(null, 2000, true)!!.startsWith("Rayon"))
        assertTrue(Zones.validationError(49, 2000, true)!!.startsWith("Rayon"))
        assertTrue(Zones.validationError(1001, 2000, true)!!.startsWith("Rayon"))
    }

    @Test
    fun `zone d'approche trop proche ou trop grande`() {
        assertEquals("Zone d'approche : au moins 550 m et au plus 5000 m", Zones.validationError(250, 549, true))
        assertTrue(Zones.validationError(250, 5001, true)!!.startsWith("Zone d'approche"))
        assertTrue(Zones.validationError(250, null, true)!!.startsWith("Zone d'approche"))
    }

    @Test
    fun `zone d'approche ignoree si le GPS precis est desactive`() =
        assertNull(Zones.validationError(250, null, twoStage = false))

    @Test
    fun `arrondi au pas des curseurs`() {
        assertEquals(250, Zones.snapRadius(250))
        assertEquals(260, Zones.snapRadius(255))
        assertEquals(50, Zones.snapRadius(10))
        assertEquals(1000, Zones.snapRadius(4000))
    }

    @Test
    fun `zone d'approche toujours au moins 300 m au-dela du rayon`() {
        assertEquals(2000, Zones.snapApproach(2000, 250))
        assertEquals(600, Zones.snapApproach(400, 250))   // min 550 → pas suivant 600
        assertEquals(700, Zones.snapApproach(640, 330))   // 600 < 630 → 700
        assertEquals(1300, Zones.snapApproach(500, 1000))
        assertEquals(5000, Zones.snapApproach(9000, 250))
        for (radius in Zones.RADIUS_MIN..Zones.RADIUS_MAX step Zones.RADIUS_STEP) {
            for (value in listOf(0, 500, 1234, 5000)) {
                val a = Zones.snapApproach(value, radius)
                assertNull("r=$radius v=$value → $a", Zones.validationError(radius, a, true))
                assertEquals("multiple du pas", 0, (a - Zones.APPROACH_MIN) % Zones.APPROACH_STEP)
            }
        }
    }

    @Test
    fun `libelles des distances`() {
        assertEquals("250 m", Zones.label(250))
        assertEquals("1 km", Zones.label(1000))
        assertEquals("2,5 km", Zones.label(2500))
    }
}
