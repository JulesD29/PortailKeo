package fr.julesdupont.portail

import fr.julesdupont.portail.SetupWizard.State
import fr.julesdupont.portail.SetupWizard.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupWizardTest {
    private val nothing = State(phoneOk = false, locationOk = false, permissionsOk = false, batteryOk = false)
    private val all = State(phoneOk = true, locationOk = true, permissionsOk = true, batteryOk = true)

    @Test
    fun `ordre des etapes`() {
        assertEquals(listOf(Step.WELCOME, Step.PHONE, Step.PERMISSIONS, Step.LOCATION, Step.BATTERY, Step.TEST), SetupWizard.steps)
        assertEquals(Step.PHONE, SetupWizard.next(Step.WELCOME))
        assertNull(SetupWizard.next(Step.TEST))
        assertNull(SetupWizard.previous(Step.WELCOME))
        assertTrue(SetupWizard.isLast(Step.TEST))
        assertEquals("Étape 3/6", SetupWizard.progress(Step.PERMISSIONS))
        assertEquals("Étape 4/6", SetupWizard.progress(Step.LOCATION))
    }

    @Test
    fun `etapes obligatoires bloquent tant qu'elles ne sont pas faites`() {
        assertTrue(SetupWizard.canGoNext(Step.WELCOME, nothing))
        assertFalse(SetupWizard.canGoNext(Step.PHONE, nothing))
        assertFalse(SetupWizard.canGoNext(Step.LOCATION, nothing))
        assertFalse(SetupWizard.canGoNext(Step.PERMISSIONS, nothing))
        assertTrue(SetupWizard.canGoNext(Step.PHONE, nothing.copy(phoneOk = true)))
        assertTrue(SetupWizard.canGoNext(Step.LOCATION, nothing.copy(locationOk = true)))
        assertTrue(SetupWizard.canGoNext(Step.PERMISSIONS, nothing.copy(permissionsOk = true)))
    }

    @Test
    fun `batterie facultative`() = assertTrue(SetupWizard.canGoNext(Step.BATTERY, nothing))

    @Test
    fun `terminer exige numero position et autorisations`() {
        assertFalse(SetupWizard.canGoNext(Step.TEST, all.copy(permissionsOk = false)))
        assertTrue(SetupWizard.canGoNext(Step.TEST, all.copy(batteryOk = false)))
    }

    @Test
    fun `autorisations demandees avant la carte`() =
        assertTrue(SetupWizard.steps.indexOf(Step.PERMISSIONS) < SetupWizard.steps.indexOf(Step.LOCATION))

    @Test
    fun `reprise a la premiere etape non faite apres un import`() {
        assertEquals(Step.PHONE, SetupWizard.resumeStep(nothing))
        // Import par QR code : numéro et position remplis → on reprend aux autorisations.
        assertEquals(Step.PERMISSIONS, SetupWizard.resumeStep(nothing.copy(phoneOk = true, locationOk = true)))
        assertEquals(Step.LOCATION, SetupWizard.resumeStep(nothing.copy(phoneOk = true, permissionsOk = true)))
        assertEquals(Step.BATTERY, SetupWizard.resumeStep(all.copy(batteryOk = false)))
        assertEquals(Step.TEST, SetupWizard.resumeStep(all))
    }
}
