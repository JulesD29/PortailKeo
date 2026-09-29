package fr.julesdupont.portail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import fr.julesdupont.portail.databinding.ActivityMapPickerBinding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Polygon
import java.io.File

/**
 * Choix du portail sur une carte OpenStreetMap : on déplace la carte sous le viseur,
 * les deux cercles (zone du portail, zone d'approche) suivent et se règlent aux curseurs.
 */
class MapPickerActivity : AppCompatActivity() {

    /** Valeurs échangées avec l'écran de réglages. */
    data class Selection(val lat: Double, val lng: Double, val radius: Int, val approach: Int)

    companion object {
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LNG = "lng"
        private const val EXTRA_RADIUS = "radius"
        private const val EXTRA_APPROACH = "approach"
        private const val EXTRA_TWO_STAGE = "twoStage"
        private const val EXTRA_HAS_POINT = "hasPoint"

        /** Centre par défaut quand aucune position n'est connue (France entière). */
        private val DEFAULT_CENTER = GeoPoint(46.6, 2.4)

        fun intent(ctx: Context, point: Pair<Double, Double>?, radius: Int, approach: Int, twoStage: Boolean) =
            Intent(ctx, MapPickerActivity::class.java)
                .putExtra(EXTRA_HAS_POINT, point != null)
                .putExtra(EXTRA_LAT, point?.first ?: 0.0)
                .putExtra(EXTRA_LNG, point?.second ?: 0.0)
                .putExtra(EXTRA_RADIUS, radius)
                .putExtra(EXTRA_APPROACH, approach)
                .putExtra(EXTRA_TWO_STAGE, twoStage)

        fun resultIntent(s: Selection) = Intent()
            .putExtra(EXTRA_LAT, s.lat).putExtra(EXTRA_LNG, s.lng)
            .putExtra(EXTRA_RADIUS, s.radius).putExtra(EXTRA_APPROACH, s.approach)

        fun parseResult(data: Intent?): Selection? {
            if (data == null || !data.hasExtra(EXTRA_LAT)) return null
            return Selection(
                data.getDoubleExtra(EXTRA_LAT, 0.0), data.getDoubleExtra(EXTRA_LNG, 0.0),
                data.getIntExtra(EXTRA_RADIUS, 200), data.getIntExtra(EXTRA_APPROACH, 2000),
            )
        }
    }

    private lateinit var b: ActivityMapPickerBinding
    private var twoStage = true
    private val gateCircle = Polygon()
    private val approachCircle = Polygon()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // osmdroid : identifiant réseau obligatoire (règles OpenStreetMap) et cache dans l'app.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
        b = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(b.root)

        twoStage = intent.getBooleanExtra(EXTRA_TWO_STAGE, true)
        val radius = Zones.snapRadius(intent.getIntExtra(EXTRA_RADIUS, 200))
        val approach = Zones.snapApproach(intent.getIntExtra(EXTRA_APPROACH, 2000), radius)
        val hasPoint = intent.getBooleanExtra(EXTRA_HAS_POINT, false)
        val start = if (hasPoint) GeoPoint(intent.getDoubleExtra(EXTRA_LAT, 0.0), intent.getDoubleExtra(EXTRA_LNG, 0.0))
                    else DEFAULT_CENTER

        setupMap(start, if (hasPoint) (if (twoStage) 13.5 else 16.0) else 6.0)
        setupSliders(radius, approach)

        b.btnMyPosition.setOnClickListener { centerOnMyPosition(animate = true) }
        b.btnCancel.setOnClickListener { finish() }
        b.btnValidate.setOnClickListener { validate() }
        if (!hasPoint) centerOnMyPosition(animate = false)
    }

    private fun setupMap(start: GeoPoint, zoom: Double) {
        val map = b.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(zoom)
        map.controller.setCenter(start)

        styleCircle(approachCircle, Color.argb(40, 232, 113, 10), Color.rgb(232, 113, 10))
        styleCircle(gateCircle, Color.argb(60, 31, 111, 235), Color.rgb(31, 111, 235))
        map.overlays.add(approachCircle)
        map.overlays.add(gateCircle)
        map.overlays.add(CopyrightOverlay(this))

        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { redraw(); return false }
            override fun onZoom(event: ZoomEvent?): Boolean { redraw(); return false }
        })
        map.post { redraw() }
    }

    private fun styleCircle(p: Polygon, fill: Int, stroke: Int) {
        p.fillPaint.color = fill
        p.outlinePaint.color = stroke
        p.outlinePaint.strokeWidth = 4f
        p.setOnClickListener { _, _, _ -> false } // pas de bulle d'info au toucher
    }

    private fun setupSliders(radius: Int, approach: Int) {
        b.sliderRadius.apply {
            valueFrom = Zones.RADIUS_MIN.toFloat(); valueTo = Zones.RADIUS_MAX.toFloat()
            stepSize = Zones.RADIUS_STEP.toFloat(); value = radius.toFloat()
            addOnChangeListener { _, v, _ ->
                val r = v.toInt()
                val a = Zones.snapApproach(b.sliderApproach.value.toInt(), r)
                if (a != b.sliderApproach.value.toInt()) b.sliderApproach.value = a.toFloat()
                redraw()
            }
        }
        b.sliderApproach.apply {
            valueFrom = Zones.APPROACH_MIN.toFloat(); valueTo = Zones.APPROACH_MAX.toFloat()
            stepSize = Zones.APPROACH_STEP.toFloat(); value = approach.toFloat()
            addOnChangeListener { _, v, fromUser ->
                val a = Zones.snapApproach(v.toInt(), b.sliderRadius.value.toInt())
                if (fromUser && a != v.toInt()) value = a.toFloat()
                redraw()
            }
        }
        val approachVisibility = if (twoStage) View.VISIBLE else View.GONE
        b.txtApproach.visibility = approachVisibility
        b.sliderApproach.visibility = approachVisibility
    }

    private fun currentSelection(): Selection {
        val c = b.map.mapCenter
        return Selection(c.latitude, c.longitude, b.sliderRadius.value.toInt(), b.sliderApproach.value.toInt())
    }

    private fun redraw() {
        val s = currentSelection()
        val center = GeoPoint(s.lat, s.lng)
        gateCircle.setPoints(Polygon.pointsAsCircle(center, s.radius.toDouble()))
        approachCircle.setPoints(Polygon.pointsAsCircle(center, s.approach.toDouble()))
        approachCircle.isEnabled = twoStage
        b.txtRadius.text = "Zone du portail (appel) : ${Zones.label(s.radius)}"
        b.txtApproach.text = "Zone d'approche (GPS précis) : ${Zones.label(s.approach)}"
        b.txtCoords.text = Coords.format(s.lat, s.lng)
        b.map.invalidate()
    }

    @SuppressLint("MissingPermission")
    private fun centerOnMyPosition(animate: Boolean) {
        if (!Perms.fineLocation(this)) {
            Toast.makeText(this, "Autorisez la localisation pour utiliser votre position", Toast.LENGTH_LONG).show()
            return
        }
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc == null || isFinishing) return@addOnSuccessListener
                val p = GeoPoint(loc.latitude, loc.longitude)
                if (b.map.zoomLevelDouble < 15) b.map.controller.setZoom(16.0)
                if (animate) b.map.controller.animateTo(p) else b.map.controller.setCenter(p)
                redraw()
            }
    }

    private fun validate() {
        setResult(Activity.RESULT_OK, resultIntent(currentSelection()))
        finish()
    }

    override fun onResume() {
        super.onResume()
        b.map.onResume()
    }

    override fun onPause() {
        b.map.onPause()
        super.onPause()
    }
}
