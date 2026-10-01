package fr.julesdupont.portail

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import fr.julesdupont.portail.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.ZoneId

/**
 * Écran principal : carte d'état en haut, bouton « Ouvrir le portail », puis les réglages
 * regroupés en cartes. Chaque modification est enregistrée immédiatement.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs
    /** Vrai pendant le remplissage du formulaire : les écouteurs ne doivent rien enregistrer. */
    private var loading = false
    private var logExpanded = false

    private val dayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    /** Réenregistrement des zones regroupé (1 s après la dernière modification). */
    private val handler = Handler(Looper.getMainLooper())
    private var geofencePending = false
    private val geofenceUpdate = Runnable { geofencePending = false; applyGeofence(silent = true) }

    private val basePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshStatus()
            if (Perms.fineLocation(this) && !Perms.backgroundLocation(this)) askBackgroundLocation()
        }

    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val s = MapPickerActivity.parseResult(result.data) ?: return@registerForActivityResult
            prefs.lat = s.lat
            prefs.lng = s.lng
            prefs.radius = s.radius
            if (prefs.twoStage) prefs.approachRadius = s.approach
            prefs.log("Position du portail modifiée sur la carte")
            loadForm()
            zoneChanged()
            snack("Position du portail enregistrée")
        }

    private val bgPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshStatus()
            applyGeofence(silent = true)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        // Premier lancement : assistant (sauf si l'app est ouverte par un QR code de configuration).
        if (!prefs.setupDone && configLink(intent) == null) {
            startActivity(SetupActivity.intent(this))
            finish()
            return
        }
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        dayLabels.forEachIndexed { i, label ->
            b.chipDays.addView(Chip(this).apply {
                setChipDrawable(ChipDrawable.createFromAttributes(
                    this@MainActivity, null, 0, com.google.android.material.R.style.Widget_Material3_Chip_Filter))
                text = label
                isCheckable = true
                id = 1000 + i + 1 // 1 = lundi … 7 = dimanche
                setOnCheckedChangeListener { chip, _ -> onDaysChanged(chip as Chip) }
            })
        }
        loadForm()
        setupListeners()
        setupTestTools()
        configLink(intent)?.let { importConfig(it) }
        Updater.scheduleDaily(this)
        if (intent.getBooleanExtra(Notifier.EXTRA_SHOW_UPDATE, false)) checkForUpdate(interactive = true)
        else showWhatsNewIfNeeded()
    }

    /** « Quoi de neuf » une seule fois après une mise à jour qui apporte des nouveautés. */
    private fun showWhatsNewIfNeeded() {
        val items = WhatsNew.toShow(prefs.lastSeenWhatsNew)
        if (items.isEmpty() || !prefs.setupDone) return
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_info)
            .setTitle("Quoi de neuf dans ${getString(R.string.app_name)}")
            .setMessage(WhatsNew.message(items))
            .setPositiveButton("C'est parti") { _, _ -> }
            .setOnDismissListener { prefs.lastSeenWhatsNew = WhatsNew.CURRENT }
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!::b.isInitialized) return
        configLink(intent)?.let { importConfig(it) }
        if (intent.getBooleanExtra(Notifier.EXTRA_SHOW_UPDATE, false)) checkForUpdate(interactive = true)
    }

    override fun onResume() {
        super.onResume()
        if (!::b.isInitialized) return
        loadForm()
        applyGeofence(silent = true)
        if (prefs.lastUpdateCheck != LocalDate.now().toString()) checkForUpdate(interactive = false)
    }

    override fun onPause() {
        // Enregistre tout de suite une modification de zone en attente.
        if (::b.isInitialized && geofencePending) {
            handler.removeCallbacks(geofenceUpdate)
            geofencePending = false
            GeofenceManager.register(this)
        }
        super.onPause()
    }

    // ---------- Écouteurs (enregistrement immédiat) ----------

    private fun setupListeners() {
        b.switchEnabled.setOnCheckedChangeListener { sw, checked ->
            if (loading) return@setOnCheckedChangeListener
            if (checked && (!prefs.hasLocation || prefs.phone.isBlank())) {
                sw.isChecked = false
                snack("Renseignez d'abord le numéro et la position du portail")
                return@setOnCheckedChangeListener
            }
            prefs.enabled = checked
            prefs.log(if (checked) "Appel automatique activé" else "Appel automatique désactivé")
            if (checked && (!Perms.backgroundLocation(this) || !Perms.call(this))) requestPermissions()
            applyGeofence(silent = false)
        }
        b.btnOpenGate.setOnClickListener { openGateNow() }
        b.btnPause.setOnClickListener { pickPauseDate() }
        b.btnResume.setOnClickListener {
            prefs.pauseUntil = ""
            prefs.log("Pause levée")
            refreshStatus()
            snack("Appel automatique repris")
        }
        b.btnPerms.setOnClickListener { requestPermissions() }
        b.btnBattery.setOnClickListener { requestBatteryExemption() }

        b.editPhone.onChange { text ->
            val phone = text.trim()
            if (phone.isEmpty()) {
                b.layoutPhone.error = "Numéro requis"
            } else {
                b.layoutPhone.error = null
                prefs.phone = phone
                refreshStatus()
            }
        }
        b.btnMap.setOnClickListener { openMap() }
        b.btnHere.setOnClickListener { useCurrentLocation() }
        b.switchTwoStage.setOnCheckedChangeListener { _, checked ->
            if (loading) return@setOnCheckedChangeListener
            prefs.twoStage = checked
            if (checked) prefs.approachRadius = Zones.snapApproach(prefs.approachRadius, prefs.radius)
            loadForm()
            zoneChanged()
        }
        b.btnAdvanced.setOnClickListener {
            val show = b.layoutAdvanced.visibility != View.VISIBLE
            b.layoutAdvanced.visibility = if (show) View.VISIBLE else View.GONE
            b.btnAdvanced.text = if (show) "Saisie manuelle ▴" else "Saisie manuelle ▾"
        }
        b.editCoords.onChange { text ->
            val coords = Coords.parse(text)
            if (coords == null) {
                b.layoutCoords.error = "Format attendu : 48.856600, 2.352200"
            } else {
                b.layoutCoords.error = null
                if (coords.first != prefs.lat || coords.second != prefs.lng || !prefs.hasLocation) {
                    prefs.lat = coords.first
                    prefs.lng = coords.second
                    zoneChanged()
                }
            }
        }
        val onRadii: (String) -> Unit = {
            val radius = b.editRadius.text?.toString()?.toIntOrNull()
            val approach = b.editApproach.text?.toString()?.toIntOrNull()
            val error = Zones.validationError(radius, approach, prefs.twoStage)
            b.layoutRadius.error = error?.takeIf { it.startsWith("Rayon") }
            b.layoutApproach.error = error?.takeIf { !it.startsWith("Rayon") }?.let { "Min. ${Zones.minApproach(radius ?: 0)} m" }
            if (error == null && (radius != prefs.radius || (prefs.twoStage && approach != prefs.approachRadius))) {
                prefs.radius = radius!!
                if (prefs.twoStage && approach != null) prefs.approachRadius = approach
                zoneChanged()
            }
        }
        b.editRadius.onChange(onRadii)
        b.editApproach.onChange(onRadii)

        b.btnStart.setOnClickListener {
            pickTime(prefs.startMinutes) { prefs.startMinutes = it; prefs.log("Début du créneau : ${Rules.fmt(it)}"); loadForm() }
        }
        b.btnEnd.setOnClickListener {
            pickTime(prefs.endMinutes) { prefs.endMinutes = it; prefs.log("Fin du créneau : ${Rules.fmt(it)}"); loadForm() }
        }
        b.switchHolidays.setOnCheckedChangeListener { _, checked ->
            if (loading) return@setOnCheckedChangeListener
            prefs.skipHolidays = checked
            prefs.log(if (checked) "Pas d'appel les jours fériés" else "Appel aussi les jours fériés")
            refreshStatus()
        }
        b.sliderHangup.addOnChangeListener { _, value, fromUser ->
            b.txtHangup.text = hangupLabel(value.toInt())
            if (fromUser) prefs.hangupSeconds = value.toInt()
        }
        b.sliderCountdown.addOnChangeListener { _, value, fromUser ->
            b.txtCountdown.text = countdownLabel(value.toInt())
            if (fromUser) prefs.countdownSeconds = value.toInt()
        }

        b.switchVoice.setOnCheckedChangeListener { _, checked ->
            if (loading) return@setOnCheckedChangeListener
            VoiceCommand.setEnabled(this, checked)
            refreshStatus()
            if (checked) snack("Dites « Ok Google, ouvre ${VoiceCommand.spokenName(this)} » (l'icône peut mettre un instant à apparaître)")
        }
        b.btnVoiceHelp.setOnClickListener { showVoiceRoutineHelp() }
        b.btnShareQr.setOnClickListener {
            if (!prefs.hasLocation || prefs.phone.isBlank()) snack("Renseignez d'abord le numéro et la position")
            else startActivity(Intent(this, ShareActivity::class.java))
        }
        b.btnScanQr.setOnClickListener { ConfigImport.scan(this) { importConfig(it) } }
        b.btnWizard.setOnClickListener { startActivity(SetupActivity.intent(this)) }
        b.btnLogMore.setOnClickListener { logExpanded = !logExpanded; refreshStatus() }
        b.btnUpdate.setOnClickListener { checkForUpdate(interactive = true) }
    }

    private fun onDaysChanged(chip: Chip) {
        if (loading) return
        val days = (1..7).filter { b.chipDays.findViewById<Chip>(1000 + it).isChecked }.toSet()
        if (days.isEmpty()) {
            loading = true; chip.isChecked = true; loading = false
            snack("Gardez au moins un jour actif")
            return
        }
        prefs.days = days
        refreshStatus()
    }

    /** Zone modifiée : met à jour l'affichage et réenregistre les zones un peu plus tard. */
    private fun zoneChanged() {
        refreshStatus()
        handler.removeCallbacks(geofenceUpdate)
        geofencePending = true
        handler.postDelayed(geofenceUpdate, 1000)
    }

    private fun EditText.onChange(block: (String) -> Unit) = addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
        override fun afterTextChanged(s: Editable?) { if (!loading) block(s?.toString().orEmpty()) }
    })

    // ---------- Partage par QR code ----------

    private fun configLink(i: Intent?): String? =
        i?.data?.takeIf { i.action == Intent.ACTION_VIEW && it.host == "config" }?.toString()

    private fun importConfig(text: String) {
        ConfigImport.confirmAndApply(this, text) {
            loadForm()
            applyGeofence(silent = true)
            if (!prefs.setupDone) {
                startActivity(SetupActivity.intent(this, resume = true))
                finish()
            } else {
                toast("Configuration importée")
                refreshStatus()
            }
        }
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
        b.btnResetSetup.setOnClickListener {
            prefs.enabled = false
            GeofenceManager.register(this) // retire les zones avant d'effacer
            TestTools.resetSetup(this)
            startActivity(SetupActivity.intent(this))
            finish()
        }
        b.btnResetWhatsNew.setOnClickListener {
            TestTools.resetWhatsNew(this)
            showWhatsNewIfNeeded()
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
            .setMessage(Updater.displayNotes(release.notes).take(1500))
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
        loading = true
        b.switchEnabled.isChecked = prefs.enabled
        if (b.editPhone.text?.toString()?.trim() != prefs.phone) b.editPhone.setText(prefs.phone)
        if (prefs.hasLocation) b.editCoords.setText(Coords.format(prefs.lat, prefs.lng))
        b.editRadius.setText(prefs.radius.toString())
        b.editApproach.setText(prefs.approachRadius.toString())
        b.layoutApproach.isEnabled = prefs.twoStage
        b.switchTwoStage.isChecked = prefs.twoStage
        for (d in 1..7) b.chipDays.findViewById<Chip>(1000 + d).isChecked = d in prefs.days
        b.switchHolidays.isChecked = prefs.skipHolidays
        b.btnStart.text = "De ${Rules.fmt(prefs.startMinutes)}"
        b.btnEnd.text = "À ${Rules.fmt(prefs.endMinutes)}"
        val countdown = prefs.countdownSeconds.coerceIn(0, 30)
        b.sliderCountdown.value = countdown.toFloat()
        b.switchVoice.isChecked = VoiceCommand.isEnabled(this)
        b.txtCountdown.text = countdownLabel(countdown)
        val hangupValue = Zones.snap(prefs.hangupSeconds, 0, 60, 5)
        b.sliderHangup.value = hangupValue.toFloat()
        b.txtHangup.text = hangupLabel(hangupValue)
        loading = false
        refreshStatus()
    }

    /** Routine Google pour une phrase exacte (« Ok Google, ouvre le portail »). */
    private fun showVoiceRoutineHelp() {
        val name = VoiceCommand.spokenName(this)
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_mic)
            .setTitle("Dire « ouvre le portail »")
            .setMessage(
                "Avec la commande vocale activée, « Ok Google, ouvre $name » fonctionne directement.\n\n" +
                "Pour une phrase à vous, créez une routine Google :\n" +
                "1. App Google Home › Automatisations › + › Personnel\n" +
                "2. Déclencheur : « Quand je dis à Google… » → « ouvre le portail »\n" +
                "3. Action : « Ajouter une commande personnalisée » → « ouvre $name »\n" +
                "4. Enregistrer.\n\n" +
                "Si le téléphone est verrouillé, l'assistant peut vous demander de le déverrouiller."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun hangupLabel(s: Int) =
        if (s == 0) "Ne pas raccrocher automatiquement" else "Raccrocher automatiquement après $s s"

    private fun countdownLabel(s: Int) =
        if (s == 0) "Appel immédiat à l'arrivée" else "Appel $s s après l'arrivée"

    private fun pickTime(current: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, current / 60, current % 60, true).show()
    }

    private fun openMap() {
        mapLauncher.launch(MapPickerActivity.intent(
            this, if (prefs.hasLocation) prefs.lat to prefs.lng else null,
            prefs.radius, prefs.approachRadius, prefs.twoStage
        ))
    }

    private fun pickPauseDate() {
        val today = LocalDate.now()
        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val until = LocalDate.of(y, m + 1, d)
            prefs.pauseUntil = until.toString()
            prefs.log("Pause jusqu'au $until inclus")
            refreshStatus()
            snack("En pause jusqu'au ${d}/${m + 1} inclus")
        }, today.year, today.monthValue - 1, today.dayOfMonth)
        dialog.datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialog.setTitle("Pas d'appel automatique jusqu'au (inclus)")
        dialog.show()
    }

    private fun applyGeofence(silent: Boolean = false) {
        GeofenceManager.register(this) { ok, msg ->
            if (isFinishing || isDestroyed) return@register
            b.txtZoneStatus.text = (if (ok) "✓ " else "✗ ") + msg
            if (!silent) snack(msg)
            refreshStatus()
        }
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        if (!Perms.fineLocation(this)) {
            snack("Autorisez d'abord la localisation")
            requestPermissions()
            return
        }
        snack("Recherche de votre position…")
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc == null) return@addOnSuccessListener snack("Position indisponible, réessayez dehors")
                prefs.lat = loc.latitude
                prefs.lng = loc.longitude
                prefs.log("Position du portail : position actuelle (±${loc.accuracy.toInt()} m)")
                loadForm()
                zoneChanged()
                snack("Portail placé ici (précision ±${loc.accuracy.toInt()} m)")
            }
            .addOnFailureListener { snack("Erreur : ${it.message}") }
    }

    /** Appel manuel immédiat, comme la tuile. */
    private fun openGateNow() {
        if (prefs.phone.isBlank()) return snack("Renseignez d'abord le numéro du portail")
        if (!Perms.call(this)) return requestPermissions()
        if (CallHelper.call(this, prefs.phone, automatic = false)) snack("Appel du portail…")
        refreshStatus()
    }

    // ---------- Autorisations ----------

    private fun requestPermissions() {
        if (!Perms.fineLocation(this) || !Perms.call(this) || !Perms.notifications(this) || !Perms.callControl(this)) {
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

    // ---------- Affichage de l'état ----------

    private fun refreshStatus() {
        val permsOk = Perms.fineLocation(this) && Perms.backgroundLocation(this) && Perms.call(this)
        val status = Dashboard.status(prefs.toRuleConfig(), prefs.hasLocation && prefs.phone.isNotBlank(),
            permsOk, AppClock.now())
        val (bgAttr, fgAttr, icon) = when (status.level) {
            Dashboard.Level.ACTIVE, Dashboard.Level.CALLED -> Triple(
                com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                if (status.level == Dashboard.Level.CALLED) R.drawable.ic_tile_phone else R.drawable.ic_check_circle)
            Dashboard.Level.PAUSED -> Triple(
                com.google.android.material.R.attr.colorTertiaryContainer,
                com.google.android.material.R.attr.colorOnTertiaryContainer, R.drawable.ic_pause)
            Dashboard.Level.OFF -> Triple(
                com.google.android.material.R.attr.colorSurfaceContainerHighest,
                com.google.android.material.R.attr.colorOnSurfaceVariant, R.drawable.ic_power)
            Dashboard.Level.WARNING, Dashboard.Level.INCOMPLETE -> Triple(
                com.google.android.material.R.attr.colorErrorContainer,
                com.google.android.material.R.attr.colorOnErrorContainer, R.drawable.ic_warning)
        }
        val bg = MaterialColors.getColor(b.cardStatus, bgAttr)
        val fg = MaterialColors.getColor(b.cardStatus, fgAttr)
        b.cardStatus.setCardBackgroundColor(bg)
        b.imgStatus.setImageResource(icon)
        b.imgStatus.imageTintList = ColorStateList.valueOf(fg)
        listOf(b.txtStatusTitle, b.txtStatusSubtitle, b.txtLastEvent, b.switchEnabled).forEach { it.setTextColor(fg) }
        b.txtStatusTitle.text = status.title
        b.txtStatusSubtitle.text = status.subtitle
        if (b.switchEnabled.isChecked != prefs.enabled) { loading = true; b.switchEnabled.isChecked = prefs.enabled; loading = false }

        b.txtLastEvent.text = Dashboard.lastEvent(prefs.logText)?.let { (stamp, msg) ->
            val time = if (Dashboard.isToday(stamp, AppClock.today())) "aujourd'hui ${stamp.substringAfter(' ')}" else stamp
            "Dernier événement ($time) : $msg"
        } ?: "Aucun événement pour l'instant"

        val paused = Rules.pauseLabel(prefs) != null
        b.btnPause.visibility = if (paused) View.GONE else View.VISIBLE
        b.btnResume.visibility = if (paused) View.VISIBLE else View.GONE

        // Alerte autorisations : seulement ce qui manque.
        val missing = buildList {
            if (!Perms.fineLocation(this@MainActivity)) add("• Localisation")
            else if (!Perms.backgroundLocation(this@MainActivity)) add("• Localisation « Toujours autoriser »")
            if (!Perms.call(this@MainActivity)) add("• Passer des appels")
            if (!Perms.notifications(this@MainActivity)) add("• Notifications (compte à rebours)")
            if (Perms.call(this@MainActivity) && !Perms.callControl(this@MainActivity))
                add("• Gestion des appels (nouvel essai, raccrochage auto)")
            if (!Perms.batteryUnrestricted(this@MainActivity)) add("• Optimisation batterie à désactiver (conseillé)")
        }
        b.cardPerms.visibility = if (missing.isEmpty()) View.GONE else View.VISIBLE
        b.txtPerms.text = missing.joinToString("\n")
        b.btnPerms.visibility =
            if (permsOk && Perms.notifications(this) && Perms.callControl(this)) View.GONE else View.VISIBLE
        b.btnBattery.visibility = if (Perms.batteryUnrestricted(this)) View.GONE else View.VISIBLE

        b.txtZoneSummary.text = if (prefs.hasLocation) {
            "Zone d'appel : ${Zones.label(prefs.radius)}" +
                (if (prefs.twoStage) " · approche : ${Zones.label(prefs.approachRadius)}" else "") +
                "\n${Coords.format(prefs.lat, prefs.lng)}"
        } else "Aucune position : placez le portail sur la carte"

        val holiday = Holidays.name(AppClock.today())
        b.txtPause.text = when {
            holiday != null && prefs.skipHolidays -> "Aujourd'hui : $holiday, pas d'appel automatique."
            else -> "Un seul appel automatique par jour."
        }

        val lines = prefs.logText.lines().filter { it.isNotBlank() }
        b.txtLog.text = (if (logExpanded) lines else lines.take(5)).joinToString("\n").ifEmpty { "—" }
        b.btnLogMore.visibility = if (lines.size > 5) View.VISIBLE else View.GONE
        b.btnLogMore.text = if (logExpanded) "Réduire" else "Tout afficher (${lines.size})"

        val voiceName = VoiceCommand.spokenName(this)
        b.switchVoice.text = "« Ok Google, ouvre $voiceName »"
        b.txtVoiceHint.text = if (VoiceCommand.isEnabled(this))
            "Activée : une icône « $voiceName » est dans vos applis. La dire à l'assistant appelle le portail sans délai."
        else "Ajoute une icône « $voiceName » à vos applis, que l'assistant vocal peut ouvrir. Pratique en conduisant."

        val calls = History.parse(prefs.history)
        b.txtHistorySummary.text = History.summary(calls, AppClock.today())
        b.txtHistory.text = History.recent(calls).joinToString("\n")
        b.txtHistory.visibility = if (calls.isEmpty()) View.GONE else View.VISIBLE

        PortalWidget.refresh(this)

        b.txtVersion.text = "Version ${Updater.currentVersionName(this)}" +
            if (TestTools.enabled) " · version de test (mises à jour depuis develop, sur demande)" else ""
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
    private fun toast(msg: String) = snack(msg)
}
