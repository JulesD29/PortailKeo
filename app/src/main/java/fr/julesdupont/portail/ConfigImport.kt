package fr.julesdupont.portail

import android.app.Activity
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Scan d'un QR code de configuration et confirmation avant import. */
object ConfigImport {

    /** Ouvre le scanner de Google Play Services (aucune autorisation caméra à demander). */
    fun scan(activity: Activity, onScanned: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(activity, options).startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(onScanned) }
            .addOnFailureListener {
                Toast.makeText(activity, "Scanner indisponible : ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    /** Vérifie le texte, affiche un résumé et applique après confirmation. */
    fun confirmAndApply(activity: Activity, text: String, onApplied: (SharedConfig) -> Unit) {
        val config = ConfigShare.decode(text)
        if (config == null) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("QR code non reconnu")
                .setMessage("Ce QR code n'est pas une configuration ${activity.getString(R.string.app_name)}.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("Importer cette configuration ?")
            .setMessage(ConfigShare.summary(config) + "\n\nVos réglages actuels seront remplacés.")
            .setPositiveButton("Importer") { _, _ ->
                ConfigShare.apply(Prefs(activity), config)
                onApplied(config)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
