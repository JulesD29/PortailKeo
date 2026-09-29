package fr.julesdupont.portail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.julesdupont.portail.databinding.ActivityShareBinding

/** Affiche la configuration actuelle en QR code, à scanner par un collègue. */
class ShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityShareBinding.inflate(layoutInflater)
        setContentView(b.root)
        // Luminosité au maximum pour faciliter le scan.
        window.attributes = window.attributes.apply { screenBrightness = 1f }

        val config = ConfigShare.fromPrefs(Prefs(this))
        val size = (resources.displayMetrics.widthPixels * 0.8).toInt()
        b.imgQr.setImageBitmap(QrCode.bitmap(ConfigShare.encode(config, BuildConfig.QR_SCHEME), size))
        b.txtSummary.text = ConfigShare.summary(config)
        b.btnClose.setOnClickListener { finish() }
    }
}
