package fr.julesdupont.portail

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Retour discret, téléphone en poche : vibrations et annonce vocale dans les écouteurs
 * aux moments clés d'un appel automatique.
 */
object Feedback {
    enum class Event { CALL_STARTED, CALL_DONE, CALL_FAILED }

    /** Quand annoncer à voix haute. */
    enum class Voice { OFF, HEADSET_ONLY, ALWAYS }

    /** Ce qu'il faut faire pour un événement (logique pure, testée à part). */
    data class Plan(val vibration: LongArray?, val speech: String?)

    val PATTERN_STARTED = longArrayOf(0, 150)
    val PATTERN_DONE = longArrayOf(0, 100, 120, 100)
    val PATTERN_FAILED = longArrayOf(0, 600)

    /**
     * @param inCall un appel est en cours : pas d'annonce (le son de l'appel occupe les écouteurs).
     */
    fun plan(event: Event, vibrate: Boolean, voice: Voice, headset: Boolean, inCall: Boolean): Plan {
        val pattern = if (!vibrate) null else when (event) {
            Event.CALL_STARTED -> PATTERN_STARTED
            Event.CALL_DONE -> PATTERN_DONE
            Event.CALL_FAILED -> PATTERN_FAILED
        }
        val text = when (event) {
            Event.CALL_STARTED -> null // l'appel va occuper le son : on annonce à la fin
            Event.CALL_DONE -> "Portail appelé"
            Event.CALL_FAILED -> "Le portail n'a pas pu être appelé"
        }
        val speak = text != null && !inCall && when (voice) {
            Voice.OFF -> false
            Voice.HEADSET_ONLY -> headset
            Voice.ALWAYS -> true
        }
        return Plan(pattern, if (speak) text else null)
    }

    // ---------- Exécution (Android) ----------

    /** Remplaçables dans les tests. */
    @Volatile var vibrator: (Context, LongArray) -> Unit = { ctx, p -> vibrateNow(ctx, p) }
    @Volatile var speaker: (Context, String) -> Unit = { ctx, t -> speakNow(ctx, t) }
    @Volatile var headsetProbe: (Context) -> Boolean = { headsetConnected(it) }
    /** Événements émis (pour les tests). */
    val emitted = mutableListOf<Event>()

    fun emit(ctx: Context, event: Event, inCall: Boolean = false) {
        val p = Prefs(ctx)
        emitted += event
        val plan = plan(event, p.feedbackVibrate, p.feedbackVoice, headsetProbe(ctx), inCall)
        plan.vibration?.let { runCatching { vibrator(ctx, it) } }
        plan.speech?.let { runCatching { speaker(ctx.applicationContext, it) } }
    }

    fun resetForTests() {
        vibrator = { ctx, p -> vibrateNow(ctx, p) }
        speaker = { ctx, t -> speakNow(ctx, t) }
        headsetProbe = { headsetConnected(it) }
        emitted.clear()
    }

    private fun vibrateNow(ctx: Context, pattern: LongArray) {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
        v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    /** Écouteurs ou casque connectés (Bluetooth, filaire, USB). */
    fun headsetConnected(ctx: Context): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return false
        val types = mutableSetOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
        if (Build.VERSION.SDK_INT >= 31) types += AudioDeviceInfo.TYPE_BLE_HEADSET
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in types }
    }

    // Synthèse vocale : moteur gardé en mémoire, initialisé à la première annonce.
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pending: String? = null

    @Synchronized
    private fun speakNow(ctx: Context, text: String) {
        val engine = tts
        if (engine != null && ttsReady) return say(engine, text)
        pending = text
        if (engine != null) return
        tts = TextToSpeech(ctx) { status ->
            synchronized(this) {
                val t = tts ?: return@synchronized
                ttsReady = status == TextToSpeech.SUCCESS
                if (!ttsReady) { Prefs(ctx).log("Annonce vocale indisponible sur ce téléphone"); return@synchronized }
                t.language = Locale.FRANCE
                t.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                pending?.let { say(t, it) }
                pending = null
            }
        }
    }

    private fun say(engine: TextToSpeech, text: String) {
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "portail-${System.currentTimeMillis()}")
    }
}
