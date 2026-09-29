package fr.julesdupont.portail

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.julesdupont.portail.SetupWizard.Step
import fr.julesdupont.portail.databinding.ActivitySetupBinding

/** Assistant de premier lancement : une étape par écran. */
class SetupActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_RESUME = "resume"

        /** @param resume true pour reprendre à la première étape non faite (après un import). */
        fun intent(ctx: Context, resume: Boolean = false) =
            Intent(ctx, SetupActivity::class.java).putExtra(EXTRA_RESUME, resume)
    }

    private lateinit var b: ActivitySetupBinding
    private lateinit var prefs: Prefs
    private var step = Step.WELCOME

    private val basePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            render()
            if (Perms.fineLocation(this) && !Perms.backgroundLocation(this)) askBackgroundLocation()
        }

    private val bgPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { render() }

    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            MapPickerActivity.parseResult(result.data)?.let { s ->
                prefs.lat = s.lat
                prefs.lng = s.lng
                prefs.radius = s.radius
                prefs.approachRadius = s.approach
                prefs.log("Assistant : position du portail choisie")
            }
            render()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)
        b.editPhone.setText(prefs.phone)
        step = if (intent.getBooleanExtra(EXTRA_RESUME, false)) SetupWizard.resumeStep(state()) else Step.WELCOME

        b.btnScan.setOnClickListener { scanColleagueConfig() }
        b.btnMap.setOnClickListener {
            mapLauncher.launch(MapPickerActivity.intent(
                this, if (prefs.hasLocation) prefs.lat to prefs.lng else null,
                prefs.radius, prefs.approachRadius, prefs.twoStage
            ))
        }
        b.btnPerms.setOnClickListener { requestPermissions() }
        b.btnBattery.setOnClickListener { requestBatteryExemption() }
        b.btnTestCall.setOnClickListener { testCall() }
        b.btnNext.setOnClickListener { goNext() }
        b.btnBack.setOnClickListener { SetupWizard.previous(step)?.let { savePhone(); step = it; render() } }
        b.editPhone.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { renderNav() }
        })
        render()
    }

    override fun onResume() {
        super.onResume()
        render() // retour des réglages système (batterie, localisation « toujours »)
    }

    private fun state() = SetupWizard.State(
        phoneOk = (if (::b.isInitialized) b.editPhone.text?.toString() else prefs.phone).orEmpty().isNotBlank(),
        locationOk = prefs.hasLocation,
        permissionsOk = Perms.fineLocation(this) && Perms.backgroundLocation(this) && Perms.call(this),
        batteryOk = Perms.batteryUnrestricted(this),
    )

    // ---------- Affichage ----------

    private fun render() {
        if (!::b.isInitialized) return
        b.txtProgress.text = SetupWizard.progress(step)
        b.progress.setProgressCompat((step.ordinal + 1) * 100 / SetupWizard.steps.size, true)
        b.txtTitle.text = step.title
        val pages = mapOf(
            Step.WELCOME to b.pageWelcome, Step.PHONE to b.pagePhone, Step.LOCATION to b.pageLocation,
            Step.PERMISSIONS to b.pagePermissions, Step.BATTERY to b.pageBattery, Step.TEST to b.pageTest,
        )
        pages.forEach { (s, v) -> v.visibility = if (s == step) View.VISIBLE else View.GONE }

        b.txtLocation.text = if (prefs.hasLocation) {
            "✓ ${Coords.format(prefs.lat, prefs.lng)}\nZone du portail : ${Zones.label(prefs.radius)}" +
                if (prefs.twoStage) " · approche : ${Zones.label(prefs.approachRadius)}" else ""
        } else "Aucune position choisie"

        fun line(ok: Boolean, label: String) = (if (ok) "✓  " else "✗  ") + label
        b.txtPerms.text = listOf(
            line(Perms.fineLocation(this), "Localisation précise"),
            line(Perms.backgroundLocation(this), "Localisation « Toujours autoriser »"),
            line(Perms.call(this), "Passer des appels"),
            line(Perms.notifications(this), "Notifications (conseillé)"),
        ).joinToString("\n")
        b.btnPerms.visibility = if (state().permissionsOk && Perms.notifications(this)) View.GONE else View.VISIBLE

        val battery = Perms.batteryUnrestricted(this)
        b.txtBattery.text = line(battery, if (battery) "Optimisation batterie désactivée" else "Optimisation batterie active")
        b.btnBattery.visibility = if (battery) View.GONE else View.VISIBLE

        val dayNames = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")
        b.txtTestSummary.text = "Jours : ${prefs.days.sorted().joinToString(", ") { dayNames[it - 1] }}" +
            " · ${Rules.fmt(prefs.startMinutes)}–${Rules.fmt(prefs.endMinutes)}" +
            " · compte à rebours ${prefs.countdownSeconds} s\n(modifiable ensuite dans les réglages)"
        renderNav()
    }

    private fun renderNav() {
        b.btnBack.visibility = if (step == Step.WELCOME) View.INVISIBLE else View.VISIBLE
        b.btnNext.text = when {
            step == Step.WELCOME -> "Configurer moi-même"
            SetupWizard.isLast(step) -> "Terminer et activer"
            step == Step.BATTERY && !Perms.batteryUnrestricted(this) -> "Passer"
            else -> "Suivant"
        }
        b.btnNext.isEnabled = SetupWizard.canGoNext(step, state())
    }

    // ---------- Navigation ----------

    private fun savePhone() {
        val phone = b.editPhone.text?.toString()?.trim().orEmpty()
        if (phone.isNotEmpty()) prefs.phone = phone
    }

    private fun goNext() {
        savePhone()
        if (!SetupWizard.canGoNext(step, state())) return
        if (SetupWizard.isLast(step)) return finishSetup()
        step = SetupWizard.next(step) ?: return
        render()
    }

    private fun finishSetup() {
        prefs.enabled = true
        prefs.setupDone = true
        prefs.log("Assistant terminé, automatisation activée")
        GeofenceManager.register(this)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    // ---------- Actions ----------

    private fun scanColleagueConfig() {
        ConfigImport.scan(this) { text ->
            ConfigImport.confirmAndApply(this, text) {
                b.editPhone.setText(prefs.phone)
                step = SetupWizard.resumeStep(state())
                render()
                Toast.makeText(this, "Configuration importée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        if (!Perms.fineLocation(this) || !Perms.call(this) || !Perms.notifications(this)) {
            basePermLauncher.launch(Perms.basePermissions())
        } else if (!Perms.backgroundLocation(this)) {
            askBackgroundLocation()
        }
    }

    private fun askBackgroundLocation() {
        if (Build.VERSION.SDK_INT < 29) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Localisation en arrière-plan")
            .setMessage("Pour détecter votre arrivée même quand l'app est fermée, choisissez « Toujours autoriser » sur l'écran suivant.")
            .setPositiveButton("Continuer") { _, _ -> bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun testCall() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Tester l'appel")
            .setMessage("Appeler ${prefs.phone} maintenant ?")
            .setPositiveButton("Appeler") { _, _ -> CallHelper.call(this, prefs.phone, automatic = false) }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
