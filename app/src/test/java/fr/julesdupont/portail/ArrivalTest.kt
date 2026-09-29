package fr.julesdupont.portail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalTest {

    @Test
    fun `dans le rayon avec une bonne precision`() = assertTrue(Arrival.isArrived(150f, 10f, 200))

    @Test
    fun `sur la limite du rayon`() = assertTrue(Arrival.isArrived(200f, 10f, 200))

    @Test
    fun `hors du rayon`() = assertFalse(Arrival.isArrived(201f, 5f, 200))

    @Test
    fun `position trop imprecise ignoree`() = assertFalse(Arrival.isArrived(50f, 150f, 80))

    @Test
    fun `tolerance egale au rayon quand il depasse 100 m`() {
        assertTrue(Arrival.isArrived(100f, 300f, 300))
        assertFalse(Arrival.isArrived(100f, 301f, 300))
    }
}
