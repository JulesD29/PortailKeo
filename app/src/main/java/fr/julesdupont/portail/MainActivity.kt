package fr.julesdupont.portail

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.julesdupont.portail.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs
    private var start = 0
    private var end = 0

    private val dayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    private val basePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshStatus()
            if (Perms.fineLocation(this) && !Perms.backgroundLocation(this)) askBackgroundLocation()
        }

    private val bgPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshStatus()
            applyGeofence()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        dayLabels.forEachIndexed { i, label ->
            b.chipDays.addView(Chip(this).apply {
                text = label
                isCheckable = true
                id = 1000 + i + 1 // 1 = lundi … 7 = dimanche
            })
        }
        loadForm()

        b.btnStart.setOnClickListener { pickTime(start) { start = it; updateTimeButtons() } }
        b.btnEnd.setOnClickListener { pickTime(end) { end = it; updateTimeButtons() } }
        b.btnHere.setOnClickListener { useCurrentLocation() }
        b.btnSave.setOnClickListener { save() }
        b.btnPerms.setOnClickListener { requestPermissions() }
        b.btnBattery.setOnClickListener { requestBatteryExemption() }
        b.btnTest.setOnClickListener { testCall() }
        b.switchTwoStage.setOnCheckedChangeListener { _, checked -> b.layoutApproach.isEnabled = checked }
        b.btnUpdate.setOnClickListener { checkForUpdate(interactive = true) }
        setupTestTools()
        Updater.scheduleDaily(this)
        if (intent.getBooleanExtra(Notifier.EXTRA_SHOW_UPDATE, false)) checkForUpdate(interactive = true)
        b.btnPause.setOnClickListener { pickPauseDate() }
        b.btnResume.setOnClickListener {
            prefs.pauseUntil = ""
            prefs.log("Pause levée")
            refreshStatus()
        }
        b.switchHolidays.setOnCheckedChangeListener { _, checked ->
            if (checked != prefs.skipHolidays) {
                prefs.skipHolidays = checked
                prefs.log(if (checked) "Jours fériés ignorés" else "Appels aussi les jours fériés")
                refreshStatus()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Notifier.EXTRA_SHOW_UPDATE, false)) checkForUpdate(interactive = true)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        applyGeofence(silent = true)
        if (prefs.lastUpdateCheck != LocalDate.now().toString()) checkForUpdate(interactive = false)
    }

    // ---------- Version de test ----------

    private fun setupTestTools() {
        if (!TestTools.enabled) return
        b.testBanner.visibility = android.view.View.VISIBLE
        b.testTools.visibility = android.view.View.VISIBLE
        b.btnSimArrival.setOnClickListener {
            TestTools.simulateArrival(this)
            toast("Arrivée simulée : voir la notification et le journal")
            refreshStatus()
        }
        b.btnSimApproach.setOnClickListener {
            TestTools.simulateApproach(this)
            toast("Approche simulée : le GPS précis mesure la distance réelle")
            refreshStatus()
        }
        b.btnResetToday.setOnClickListener {
            TestTools.resetToday(this)
            toast("Appel du jour réinitialisé")
            refreshStatus()
        }
    }

    // ---------- Mises à jour ----------

    private fun checkForUpdate(interactive: Boolean) {
        if (interactive) toast("Recherche d'une mise à jour…")
        Updater.check(this, interactive) { result ->
            if (isFinishing || isDestroyed) return@check
            result.onFailure { if (interactive) toast("Impossible de vérifier : ${it.message}") }
            result.onSuccess { release ->
                if (release != null) showUpdateDialog(release)
                else if (interactive) toast("${getString(R.string.app_name)} est à jour")
            }
        }
    }

    private fun showUpdateDialog(release: Updater.Release) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Nouvelle version ${release.name}")
            .setMessage(release.notes.ifBlank { "Une nouvelle version est disponible." }.take(1000))
            .setPositiveButton("Installer") { _, _ -> installUpdate(release) }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    private fun installUpdate(release: Updater.Release) {
        if (!packageManager.canRequestPackageInstalls()) {
            toast("Autorisez « Installer des applis inconnues » pour ${getString(R.string.app_name)}, puis réessayez")
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        toast("Téléchargement de la version ${release.name}…")
        Updater.downloadAndInstall(this, release) { err -> toast("Échec de la mise à jour : $err") }
    }

    // ---------- Formulaire ----------

    private fun loadForm() {
        b.switchEnabled.isChecked = prefs.enabled
        b.editPhone.setText(prefs.phone)
        if (prefs.hasLocation) b.editCoords.setText(Coords.format(prefs.lat, prefs.lng))
        b.editRadius.setText(prefs.radius.toString())
        b.switchTwoStage.isChecked = prefs.twoStage
        b.editApproach.setText(prefs.approachRadius.toString())
        b.layoutApproach.isEnabled = prefs.twoStage
        b.editCountdown.setText(prefs.countdownSeconds.toString())
        val days = prefs.days
        for (d in 1..7) (b.chipDays.findViewById<Chip>(1000 + d)).isChecked = d in days
        b.switchHolidays.isChecked = prefs.skipHolidays
        start = prefs.startMinutes
        end = prefs.endMinutes
        updateTimeButtons()
    }

    private fun updateTimeButtons() {
        b.btnStart.text = "De ${Rules.fmt(start)}"
        b.btnEnd.text = "À ${Rules.fmt(end)}"
    }

    private fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, true).show()
    }

    private fun pickPauseDate() {
        val today = LocalDate.now()
        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val until = LocalDate.of(y, m + 1, d)
            prefs.pauseUntil = until.toString()
            prefs.log("Pause jusqu'au $until inclus")
            refreshStatus()
        }, today.year, today.monthValue - 1, today.dayOfMonth)
        dialog.datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialog.setTitle("Pas d'appel automatique jusqu'au (inclus)")
        dialog.show()
    }

    private fun save() {
        val phone = b.editPhone.text?.toString()?.trim().orEmpty()
        val coords = Coords.parse(b.editCoords.text?.toString().orEmpty())
        val radius = b.editRadius.text?.toString()?.toIntOrNull()
        val twoStage = b.switchTwoStage.isChecked
        val approach = b.editApproach.text?.toString()?.toIntOrNull()
        val countdown = b.editCountdown.text?.toString()?.toIntOrNull()
        val days = (1..7).filter { b.chipDays.findViewById<Chip>(1000 + it).isChecked }.toSet()

        when {
            phone.isEmpty() -> return toast("Indiquez le numéro du portail")
            coords == null -> return toast("Coordonnées invalides (ex. 48.856600, 2.352200)")
            radius == null || radius < 50 || radius > 5000 -> return toast("Rayon entre 50 et 5000 m")
            days.isEmpty() -> return toast("Choisissez au moins un jour")
            twoStage && (approach == null || approach < radius!! + 300 || approach > 20000) ->
                return toast("Zone d'approche : au moins ${radius!! + 300} m et au plus 20 000 m")
            countdown == null || countdown < 0 || countdown > 60 -> return toast("Compte à rebours entre 0 et 60 s")
        }
        prefs.phone = phone
        prefs.lat = coords!!.first
        prefs.lng = coords.second
        prefs.radius = radius!!
        prefs.days = days
        prefs.twoStage = twoStage
        if (approach != null) prefs.approachRadius = approach
        prefs.countdownSeconds = countdown!!
        prefs.startMinutes = start
        prefs.endMinutes = end
        prefs.enabled = b.switchEnabled.isChecked
        prefs.log("Réglages enregistrés (${if (prefs.enabled) "activé" else "désactivé"})")

        if (prefs.enabled && (!Perms.backgroundLocation(this) || !Perms.call(this))) {
            requestPermissions()
        }
        applyGeofence()
    }

    private fun applyGeofence(silent: Boolean = false) {
        GeofenceManager.register(this) { ok, msg ->
            b.txtZoneStatus.text = (if (ok) "✓ " else "✗ ") + msg
            if (!silent) toast(msg)
            refreshStatus()
        }
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        if (!Perms.fineLocation(this)) {
            toast("Autorisez d'abord la localisation")
            requestPermissions()
            return
        }
        toast("Recherche de la position…")
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc == null) toast("Position indisponible, réessayez dehors")
                else {
                    b.editCoords.setText(Coords.format(loc.latitude, loc.longitude))
                    toast("Position trouvée (±${loc.accuracy.toInt()} m). Pensez à enregistrer.")
                }
            }
            .addOnFailureListener { toast("Erreur : ${it.message}") }
    }

    // ---------- Autorisations ----------

    private fun requestPermissions() {
        if (!Perms.fineLocation(this) || !Perms.call(this) || !Perms.notifications(this)) {
            basePermLauncher.launch(Perms.basePermissions())
        } else if (!Perms.backgroundLocation(this)) {
            askBackgroundLocation()
        } else {
            toast("Toutes les autorisations sont accordées")
        }
    }

    private fun askBackgroundLocation() {
        if (Build.VERSION.SDK_INT < 29) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Localisation en arrière-plan")
            .setMessage("Pour détecter votre arrivée même quand l'app est fermée, choisissez « Toujours autoriser » sur l'écran suivant.")
            .setPositiveButton("Continuer") { _, _ ->
                bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        if (Perms.batteryUnrestricted(this)) return toast("Déjà désactivée")
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun refreshStatus() {
        fun line(ok: Boolean, label: String) = (if (ok) "✓  " else "✗  ") + label
        b.txtPerms.text = listOf(
            line(Perms.fineLocation(this), "Localisation précise"),
            line(Perms.backgroundLocation(this), "Localisation « Toujours autoriser »"),
            line(Perms.call(this), "Passer des appels"),
            line(Perms.notifications(this), "Notifications"),
            line(Perms.batteryUnrestricted(this), "Optimisation batterie désactivée (recommandé)"),
        ).joinToString("\n")
        b.txtLog.text = prefs.logText.ifEmpty { "—" }

        val pause = Rules.pauseLabel(prefs)
        val holiday = Holidays.name(LocalDate.now())
        b.txtPause.text = when {
            pause != null -> "⏸  Automatisation ${pause}"
            holiday != null && prefs.skipHolidays -> "Aujourd'hui : $holiday, pas d'appel automatique"
            else -> "Aucune pause en cours"
        }
        b.btnResume.isEnabled = pause != null
        b.txtVersion.text = "Version installée : ${Updater.currentVersionName(this)}" +
            if (TestTools.enabled) " (version de test : mises à jour depuis develop, sur demande)" else ""
    }

    // ---------- Test ----------

    private fun testCall() {
        val phone = b.editPhone.text?.toString()?.trim().orEmpty()
        if (phone.isEmpty()) return toast("Indiquez le numéro du portail")
        if (!Perms.call(this)) return requestPermissions()
        MaterialAlertDialogBuilder(this)
            .setTitle("Tester l'appel")
            .setMessage("Appeler $phone maintenant ?")
            .setPositiveButton("Appeler") { _, _ ->
                CallHelper.call(this, phone, automatic = false)
                refreshStatus()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
