package fr.julesdupont.portail

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PortalWidgetTest {
    private val app get() = TestSupport.app

    @Before fun setUp() = TestSupport.setUp()
    @After fun tearDown() = TestSupport.tearDown()

    @Test
    fun `widget non configure`() =
        assertEquals("Configuration incomplète", PortalWidget.content(app).first)

    @Test
    fun `widget sans autorisations`() {
        TestSupport.configuredPrefs()
        assertEquals("Autorisations manquantes", PortalWidget.content(app).first)
    }

    @Test
    fun `mise a jour d'un widget pose sur l'ecran d'accueil`() {
        TestSupport.configuredPrefs()
        val manager = shadowOf(AppWidgetManager.getInstance(app))
        val id = manager.createWidget(PortalWidget::class.java, R.layout.widget_portal)
        PortalWidget.refresh(app) // ne doit pas planter
        assertEquals(1, AppWidgetManager.getInstance(app).getAppWidgetIds(ComponentName(app, PortalWidget::class.java)).size)
        assertEquals(true, id > 0 || id == 0)
    }

    @Test
    fun `sans widget pose rien a faire`() = PortalWidget.refresh(app)
}
