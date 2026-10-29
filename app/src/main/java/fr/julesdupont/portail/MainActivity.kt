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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
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
import fr.julesdupont.portail.databinding.ItemDayBinding
import fr.julesdupont.portail.databinding.ItemLogBinding
import fr.julesdupont.portail.databinding.RowSettingBinding
import java.time.LocalDate
import java.time.ZoneId

/**
 * Écran principal en trois onglets :
 * - Accueil : carte d'état, gros bouton « Ouvrir le portail », suivi en direct du compte à rebours et de l'appel ;
 * - Réglages : une entrée par thème, chacune ouvre sa page (une section de [ActivityMainBinding.pageDetail]) ;
 * - Historique : semaine en cours et journal.
 * Chaque modification est enregistrée immédiatement.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs
    /** Vrai pendant le remplissage du formulaire : les écouteurs ne doivent rien enregistrer. */
    private var loading = false
    private var logExpanded = false

    enum class Tab { HOME, SETTINGS, HISTORY }
    private var tab = Tab.HOME
    /** Section affichée dans la page d'un réglage, null = liste des réglages. */
    private var openSection: View? = null

    /** Retour : ferme la page d'un réglage, puis revient à l'accueil. */
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (openSection != null) closeDetail() else b.bottomNav.selectedItemId = R.id.navHome
        }
    }

    /** Rafraîchit le suivi en direct (compte à rebours, appel) tant que l'écran est visible. */
    private val liveTick = object : Runnable {
        override fun run() {
            renderLive()
            handler.postDelayed(this, 500)
        }
    }
    private var liveShown = false

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
        setupNavigation()
        setupTestTools()
        savedInstanceState?.getString(STATE_TAB)?.let { name ->
            b.bottomNav.selectedItemId = when (name) {
                Tab.SETTINGS.name -> R.id.navSettings
                Tab.HISTORY.name -> R.id.navHistory
                else -> R.id.navHome
            }
        }
        configLink(intent)?.let { importConfig(it) }
        Updater.scheduleDaily(this)
        if (intent.getBooleanExtra(Notifier.EXTRA_SHOW_UPDATE, false)) checkForUpdate(interactive = true)
        else showWhatsNewIfNeeded()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAB, tab.name)
    }

    /** « Quoi de neuf » une seule fois après une mise à jour qui apporte des nouveautés. */
    private fun showWhatsNewIfNeeded() {
        val items = WhatsNew.toShow(prefs.lastSeenWhatsNew)
        if (items.isEmpty() || !prefs.setupDone) return
        showWhatsNew(items)
    }

    private fun showWhatsNew(items: List<WhatsNew.Item>) {
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
        handler.post(liveTick)
    }

    override fun onPause() {
        handler.removeCallbacks(liveTick)
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
            prefs.log("Pause terminée")
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
        val onFeedbackChanged = { _: android.widget.CompoundButton, _: Boolean ->
            if (!loading) {
                prefs.feedbackVibrate = b.switchFeedbackVibrate.isChecked
                prefs.feedbackVoice = when {
                    !b.switchFeedbackVoice.isChecked -> Feedback.Voice.OFF
                    b.switchFeedbackHeadset.isChecked -> Feedback.Voice.HEADSET_ONLY
                    else -> Feedback.Voice.ALWAYS
                }
                b.switchFeedbackHeadset.isEnabled = b.switchFeedbackVoice.isChecked
            }
        }
        b.switchFeedbackVibrate.setOnCheckedChangeListener(onFeedbackChanged)
        b.switchFeedbackVoice.setOnCheckedChangeListener(onFeedbackChanged)
        b.switchFeedbackHeadset.setOnCheckedChangeListener(onFeedbackChanged)
        b.btnFeedbackTry.setOnClickListener {
            if (prefs.feedbackVoice == Feedback.Voice.HEADSET_ONLY && !Feedback.headsetConnected(this))
                snack("Pas d'écouteurs connectés : vibration seulement")
            Feedback.emit(this, Feedback.Event.CALL_DONE)
        }
        b.switchRetryBusy.setOnCheckedChangeListener { _, checked ->
            if (loading) return@setOnCheckedChangeListener
            prefs.retryIfBusy = checked
            prefs.log(if (checked) "Rappel si le portail est occupé : activé" else "Rappel si le portail est occupé : désactivé")
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
        b.btnShareQr.setOnClickListener { shareConfig() }
        b.btnScanQr.setOnClickListener { ConfigImport.scan(this) { importConfig(it) } }
        b.btnWizard.setOnClickListener { startActivity(SetupActivity.intent(this)) }
        b.btnLogMore.setOnClickListener { logExpanded = !logExpanded; refreshHistory() }
        b.btnUpdate.setOnClickListener { checkForUpdate(interactive = true) }
        b.btnWhatsNew.setOnClickListener { showWhatsNew(WhatsNew.items.filter { it.edition == WhatsNew.CURRENT }) }
        b.btnLiveAction.setOnClickListener {
            when (LiveCall.display(LiveCall.state)?.action) {
                LiveCall.Action.CANCEL -> PortalService.cancel(this)
                LiveCall.Action.HANG_UP -> PortalService.hangUp(this)
                else -> {}
            }
        }
    }

    private fun shareConfig() {
        if (!prefs.hasLocation || prefs.phone.isBlank()) snack("Renseignez d'abord le numéro et la position")
        else startActivity(Intent(this, ShareActivity::class.java))
    }

    // ---------- Onglets et pages des réglages ----------

    private val sections get() = listOf(b.secPortal, b.secSchedule, b.secCall, b.secFeedback, b.secVoice,
        b.secShare, b.testTools, b.secAbout)

    private fun setupNavigation() {
        onBackPressedDispatcher.addCallback(this, backCallback)
        b.bottomNav.setOnItemSelectedListener { item ->
            showTab(when (item.itemId) {
                R.id.navSettings -> Tab.SETTINGS
                R.id.navHistory -> Tab.HISTORY
                else -> Tab.HOME
            })
            true
        }
        b.bottomNav.setOnItemReselectedListener { if (it.itemId == R.id.navSettings && openSection != null) closeDetail() }
        b.detailToolbar.setNavigationOnClickListener { closeDetail() }
        b.cardLastCall.setOnClickListener { b.bottomNav.selectedItemId = R.id.navHistory }
        b.btnHomeMap.setOnClickListener { openMap() }
        b.btnHeaderShare.setOnClickListener { shareConfig() }

        row(b.rowPhone, R.drawable.ic_tile_phone, "Numéro du portail") { openDetail(b.secPortal, "Portail") }
        row(b.rowZone, R.drawable.ic_location, "Position et zones") { openDetail(b.secPortal, "Portail") }
        row(b.rowSchedule, R.drawable.ic_schedule, "Jours et horaires") { openDetail(b.secSchedule, "Quand appeler") }
        row(b.rowCall, R.drawable.ic_timer, "Appel automatique") { openDetail(b.secCall, "Appel automatique") }
        row(b.rowFeedback, R.drawable.ic_headphones, "Retour vocal") { openDetail(b.secFeedback, "Retour vocal") }
        row(b.rowShortcuts, R.drawable.ic_mic, "Tuile, widget et commande vocale") { openDetail(b.secVoice, "Accès rapide") }
        row(b.rowShare, R.drawable.ic_qr_code, "Partager avec un collègue") { openDetail(b.secShare, "Partager avec un collègue") }
        row(b.rowPerms, R.drawable.ic_check_circle, "Autorisations") {
            when {
                missingPermissions().isEmpty() -> toast("Toutes les autorisations sont accordées")
                basePermissionsOk() -> requestBatteryExemption()
                else -> requestPermissions()
            }
        }
        row(b.rowTest, R.drawable.ic_build, "Outils de test") { openDetail(b.testTools, "Outils de test") }
        b.rowTest.root.visibility = if (TestTools.enabled) View.VISIBLE else View.GONE
        row(b.rowAbout, R.drawable.ic_info, "À propos et mises à jour") { openDetail(b.secAbout, "À propos") }
        showTab(Tab.HOME)
    }

    private fun row(r: RowSettingBinding, icon: Int, title: String, onClick: () -> Unit) {
        r.rowIcon.setImageResource(icon)
        r.rowTitle.text = title
        r.root.setOnClickListener { onClick() }
    }

    private fun showTab(t: Tab) {
        tab = t
        if (t != Tab.SETTINGS) openSection = null
        b.pageHome.visibility = if (t == Tab.HOME) View.VISIBLE else View.GONE
        b.pageSettings.visibility = if (t == Tab.SETTINGS && openSection == null) View.VISIBLE else View.GONE
        b.pageDetail.visibility = if (t == Tab.SETTINGS && openSection != null) View.VISIBLE else View.GONE
        b.pageHistory.visibility = if (t == Tab.HISTORY) View.VISIBLE else View.GONE
        if (t == Tab.HISTORY) refreshHistory()
        updateBack()
    }

    private fun openDetail(section: View, title: String) {
        sections.forEach { it.visibility = if (it === section) View.VISIBLE else View.GONE }
        b.detailToolbar.title = title
        b.detailScroll.scrollTo(0, 0)
        openSection = section
        b.pageSettings.visibility = View.GONE
        b.pageDetail.visibility = View.VISIBLE
        updateBack()
    }

    private fun closeDetail() {
        openSection = null
        b.pageDetail.visibility = View.GONE
        b.pageSettings.visibility = View.VISIBLE
        refreshStatus() // résumés des réglages à jour
        updateBack()
    }

    private fun updateBack() {
        backCallback.isEnabled = tab != Tab.HOME || openSection != null
    }

    // ---------- Suivi en direct ----------

    @VisibleForTesting
    internal fun renderLive() {
        val d = LiveCall.display(LiveCall.state)
        val show = d != null
        if (show != liveShown) {
            liveShown = show
            if (!show) refreshStatus() // l'appel du jour a pu changer l'état
        }
        b.cardStatus.visibility = if (show) View.GONE else View.VISIBLE
        b.layoutGate.visibility = if (show) View.GONE else View.VISIBLE
        b.cardLive.visibility = if (show) View.VISIBLE else View.GONE
        b.layoutLive.visibility = if (show) View.VISIBLE else View.GONE
        if (d == null) return
        b.liveBadge.text = d.badge
        b.liveTitle.text = d.title
        b.liveSubtitle.text = d.subtitle
        b.liveBig.text = d.big
        b.liveSmall.text = d.small
        b.liveSmall.visibility = if (d.small.isEmpty()) View.GONE else View.VISIBLE
        b.liveProgress.setProgressCompat(d.progress ?: 100, true)
        listOf(b.liveStep1, b.liveStep2, b.liveStep3).forEachIndexed { i, step -> styleStep(step, i < d.steps) }
        b.btnLiveAction.visibility = if (d.action == LiveCall.Action.NONE) View.GONE else View.VISIBLE
        b.btnLiveAction.text = if (d.action == LiveCall.Action.CANCEL) "Annuler l'appel" else "Raccrocher maintenant"
    }

    private fun styleStep(step: TextView, done: Boolean) {
        val bg = MaterialColors.getColor(step, if (done) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerHighest)
        val fg = MaterialColors.getColor(step, if (done) com.google.android.material.R.attr.colorOnPrimaryContainer
            else com.google.android.material.R.attr.colorOnSurfaceVariant)
        step.backgroundTintList = ColorStateList.valueOf(bg)
        step.setTextColor(fg)
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
        b.btnSimArrivalLocked.setOnClickListener {
            TestTools.simulateArrivalDelayed(this, 20)
            snack("Verrouillez le téléphone maintenant : l'appel partira dans 20 s")
            refreshStatus()
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
        b.switchFeedbackVibrate.isChecked = prefs.feedbackVibrate
        b.switchFeedbackVoice.isChecked = prefs.feedbackVoice != Feedback.Voice.OFF
        b.switchFeedbackHeadset.isChecked = prefs.feedbackVoice != Feedback.Voice.ALWAYS
        b.switchFeedbackHeadset.isEnabled = prefs.feedbackVoice != Feedback.Voice.OFF
        val hangupValue = Zones.snap(prefs.hangupSeconds, 0, 60, 5)
        b.sliderHangup.value = hangupValue.toFloat()
        b.txtHangup.text = hangupLabel(hangupValue)
        b.switchRetryBusy.isChecked = prefs.retryIfBusy
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
        dialog.setTitle("Mettre en pause jusqu'au… (inclus)")
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
        } else if (!Perms.fullScreen(this) && Build.VERSION.SDK_INT >= 34) {
            snack("Autorisez les notifications plein écran pour appeler téléphone verrouillé")
            startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
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
        val missing = missingPermissions()
        b.cardPerms.visibility = if (missing.isEmpty()) View.GONE else View.VISIBLE
        b.txtPerms.text = missing.joinToString("\n")
        b.btnPerms.visibility = if (basePermissionsOk()) View.GONE else View.VISIBLE
        b.btnBattery.visibility = if (Perms.batteryUnrestricted(this)) View.GONE else View.VISIBLE

        b.txtZoneSummary.text = if (prefs.hasLocation) {
            "Zone du portail : ${Zones.label(prefs.radius)}" +
                (if (prefs.twoStage) " · approche : ${Zones.label(prefs.approachRadius)}" else "") +
                "\n${Coords.format(prefs.lat, prefs.lng)}"
        } else "Aucune position : placez le portail sur la carte"

        val holiday = Holidays.name(AppClock.today())
        b.txtPause.text = when {
            holiday != null && prefs.skipHolidays -> "Aujourd'hui : $holiday, pas d'appel automatique."
            else -> "Un seul appel automatique par jour."
        }

        val voiceName = VoiceCommand.spokenName(this)
        b.switchVoice.text = "« Ok Google, ouvre $voiceName »"
        b.txtVoiceHint.text = if (VoiceCommand.isEnabled(this))
            "Activée : une icône « $voiceName » est dans vos applis. La dire à l'assistant appelle le portail sans délai."
        else "Ajoute une icône « $voiceName » à vos applis, que l'assistant vocal peut ouvrir. Pratique en conduisant."

        b.txtTileHint.text = "Tuile : ouvrez les réglages rapides, touchez le crayon et ajoutez « ${getString(R.string.tile_label)} ».\n" +
            "Widget : appui long sur l'écran d'accueil › Widgets › ${getString(R.string.app_name)}."

        val calls = History.parse(prefs.history)
        b.txtLastCall.text = calls.firstOrNull()?.let { "Dernier appel : ${History.recent(listOf(it), 1).first()}" }
            ?: "Aucun appel automatique pour l'instant"
        b.txtLastCallSub.text = if (calls.isEmpty()) "L'historique et le journal sont ici" else History.summary(calls, AppClock.today())

        // Résumés de l'onglet Réglages
        b.rowPhone.rowSubtitle.text = Summaries.phone(prefs.phone)
        b.rowZone.rowSubtitle.text = Summaries.zone(prefs.hasLocation, prefs.radius, prefs.twoStage, prefs.approachRadius)
        b.rowSchedule.rowSubtitle.text = Summaries.schedule(prefs.days, prefs.startMinutes, prefs.endMinutes)
        b.rowCall.rowSubtitle.text = Summaries.call(prefs.countdownSeconds, prefs.hangupSeconds)
        b.rowFeedback.rowSubtitle.text = Summaries.feedback(prefs.feedbackVibrate, prefs.feedbackVoice)
        b.rowShortcuts.rowSubtitle.text = Summaries.shortcuts(VoiceCommand.isEnabled(this), voiceName)
        b.rowShare.rowSubtitle.text = "QR code ou import d'une configuration"
        b.rowPerms.rowSubtitle.text = Summaries.permissions(missing.size)
        b.rowPerms.rowIcon.setImageResource(if (missing.isEmpty()) R.drawable.ic_check_circle else R.drawable.ic_warning)
        b.rowTest.rowSubtitle.text = "Simuler l'arrivée, réinitialiser l'appel du jour…"
        b.rowAbout.rowSubtitle.text = "Version ${Updater.currentVersionName(this)} · Quoi de neuf"

        if (tab == Tab.HISTORY) refreshHistory()

        PortalWidget.refresh(this)

        b.txtVersion.text = "Version ${Updater.currentVersionName(this)}" +
            if (TestTools.enabled) " · version de test (mises à jour depuis develop, sur demande)" else ""
    }

    /** Onglet Historique : semaine en cours et journal. */
    private fun refreshHistory() {
        val today = AppClock.today()
        val calls = History.parse(prefs.history)
        b.txtHistorySummary.text = History.summary(calls, today)

        b.weekRow.removeAllViews()
        History.week(calls, today, prefs.days).forEach { d ->
            val cell = ItemDayBinding.inflate(layoutInflater, b.weekRow, false)
            cell.dayLabel.text = d.label
            cell.dayTime.text = d.time ?: when {
                d.future -> "à venir"
                d.isToday -> "aujourd'hui"
                else -> "—"
            }
            val (dotBg, dotFg) = if (d.time != null)
                com.google.android.material.R.attr.colorPrimary to com.google.android.material.R.attr.colorOnPrimary
            else com.google.android.material.R.attr.colorSurfaceContainerHighest to com.google.android.material.R.attr.colorOnSurfaceVariant
            cell.dayDot.text = if (d.time != null) "✓" else if (d.future || d.isToday) "" else "–"
            cell.dayDot.backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(cell.dayDot, dotBg))
            cell.dayDot.setTextColor(MaterialColors.getColor(cell.dayDot, dotFg))
            if (d.isToday) {
                cell.root.setBackgroundResource(R.drawable.bg_rounded)
                cell.root.backgroundTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(cell.root, com.google.android.material.R.attr.colorPrimaryContainer))
            }
            cell.root.contentDescription = "${d.label} : ${d.time?.let { "appel à $it" } ?: "pas d'appel"}"
            b.weekRow.addView(cell.root)
        }

        val entries = Journal.parse(prefs.logText, today)
        b.logList.removeAllViews()
        if (entries.isEmpty()) {
            b.logList.addView(TextView(this).apply {
                text = "Aucun événement pour l'instant"
                setPadding(0, 16, 0, 16)
            })
        }
        (if (logExpanded) entries else entries.take(LOG_PREVIEW)).forEach { e ->
            val row = ItemLogBinding.inflate(layoutInflater, b.logList, false)
            row.logTime.text = if (e.date != null) "${e.date}\n${e.time}" else e.time
            row.logMessage.text = e.message
            val dot = when {
                e.message.startsWith("Échec") || e.message.startsWith("Erreur") -> com.google.android.material.R.attr.colorError
                e.message.contains("ppel") || e.message.contains("accroch") -> com.google.android.material.R.attr.colorPrimary
                else -> com.google.android.material.R.attr.colorOutline
            }
            row.logDot.backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(row.logDot, dot))
            b.logList.addView(row.root)
        }
        b.btnLogMore.visibility = if (entries.size > LOG_PREVIEW) View.VISIBLE else View.GONE
        b.btnLogMore.text = if (logExpanded) "Réduire" else "Tout afficher (${entries.size})"
    }

    private fun basePermissionsOk() = Perms.fineLocation(this) && Perms.backgroundLocation(this) && Perms.call(this) &&
        Perms.notifications(this) && Perms.callControl(this) && Perms.fullScreen(this)

    /** Ce qui manque pour que l'app fonctionne, une ligne par réglage. */
    private fun missingPermissions(): List<String> = buildList {
        if (!Perms.fineLocation(this@MainActivity)) add("• Localisation")
        else if (!Perms.backgroundLocation(this@MainActivity)) add("• Localisation « Toujours autoriser »")
        if (!Perms.call(this@MainActivity)) add("• Passer des appels")
        if (!Perms.notifications(this@MainActivity)) add("• Notifications (compte à rebours)")
        if (Perms.call(this@MainActivity) && !Perms.callControl(this@MainActivity))
            add("• Gestion des appels (rappeler, raccrocher tout seul)")
        if (!Perms.fullScreen(this@MainActivity)) add("• Notifications plein écran (appel téléphone verrouillé)")
        if (!Perms.batteryUnrestricted(this@MainActivity)) add("• Optimisation de la batterie à désactiver (conseillé)")
    }

    private fun snack(msg: String) = Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).setAnchorView(b.bottomNav).show()
    private fun toast(msg: String) = snack(msg)

    companion object {
        private const val STATE_TAB = "tab"
        private const val LOG_PREVIEW = 8
    }
}
