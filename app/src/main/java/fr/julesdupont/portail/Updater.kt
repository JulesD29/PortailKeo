package fr.julesdupont.portail

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Mises à jour depuis les « Releases » du dépôt GitHub public, comme Metrolist : l'app cherche une nouvelle
 * version et la signale (fenêtre, notification), puis ouvre le lien de l'APK dans le navigateur. C'est le
 * navigateur qui télécharge et l'installateur d'Android qui installe : l'app n'a pas l'autorisation d'installer
 * des applis (REQUEST_INSTALL_PACKAGES), qui déclenchait les alertes de Play Protect et des antivirus.
 */
object Updater {
    private const val REPO = "JulesD29/PortailKeo"
    const val ACTION_CHECK = "fr.julesdupont.portail.UPDATE_CHECK"
    const val RELEASES_URL = "https://github.com/$REPO/releases"

    /** Pas plus d'une vérification automatique par heure (l'API GitHub limite les appels). */
    const val MIN_INTERVAL_MS = 60 * 60 * 1000L

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    data class Release(val versionCode: Long, val name: String, val apkUrl: String, val notes: String)

    fun currentVersionCode(ctx: Context): Long =
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))

    fun currentVersionName(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"

    /** Dernière release publiée (appel bloquant), ou null s'il n'y en a pas. */
    private fun fetchLatest(): Release? {
        // App normale : dernière Release publiée depuis main.
        // Version de test : pré-release « test » publiée depuis develop.
        val path = if (TestTools.enabled) "releases/tags/test" else "releases/latest"
        val c = (URL("https://api.github.com/repos/$REPO/$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "PortailKeo")
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (c.responseCode == 404) return null
            if (c.responseCode != 200) throw IOException("serveur de mises à jour injoignable (erreur ${c.responseCode})")
            return parseRelease(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    /**
     * Lit la réponse JSON de l'API GitHub. Numéro de version : chiffres du tag (« v12 »),
     * sinon dernier nombre du nom (« Test 1.15 » pour la version de test). Null si pas d'APK ou pas de numéro.
     */
    fun parseRelease(body: String): Release? {
        val json = JSONObject(body)
        val tag = json.getString("tag_name")
        val code = tag.filter { it.isDigit() }.toLongOrNull()
            ?: Regex("\\d+").findAll(json.optString("name")).lastOrNull()?.value?.toLongOrNull()
            ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        var apk: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk")) {
                apk = a.getString("browser_download_url"); break
            }
        }
        val name = json.optString("name").ifBlank { tag }
        return Release(code, name, apk ?: return null, json.optString("body", ""))
    }

    /** Notes de version sans titres Markdown, pour la fenêtre de mise à jour. */
    fun displayNotes(notes: String): String =
        notes.lines().filterNot { it.trimStart().startsWith("#") }.joinToString("\n").trim()
            .ifBlank { "Une nouvelle version est disponible." }

    /** Vrai si la release est plus récente que la version installée. */
    fun isNewer(release: Release, installedVersionCode: Long) = release.versionCode > installedVersionCode

    /** Vérification automatique : au plus une par heure ; sur demande, toujours. */
    fun shouldCheck(lastCheckMs: Long, nowMs: Long, interactive: Boolean): Boolean =
        interactive || nowMs - lastCheckMs !in 0 until MIN_INTERVAL_MS

    /** Proposer automatiquement cette version ? Non si l'utilisateur a déjà répondu « Plus tard » pour elle. */
    fun shouldOffer(release: Release, dismissedVersionCode: Long): Boolean = release.versionCode > dismissedVersionCode

    /**
     * Cherche une version plus récente que celle installée ; callback sur le thread principal.
     * @param interactive true si l'utilisateur a touché « Rechercher une mise à jour ».
     * La version de test ne cherche que sur demande (pas de vérification automatique).
     */
    fun check(ctx: Context, interactive: Boolean = false, callback: (Result<Release?>) -> Unit) {
        val app = ctx.applicationContext
        val prefs = Prefs(app)
        if ((TestTools.enabled && !interactive) ||
            !shouldCheck(prefs.lastUpdateCheckMs, System.currentTimeMillis(), interactive)) {
            main.post { callback(Result.success(null)) }
            return
        }
        io.execute {
            val result = runCatching { fetchLatest()?.takeIf { isNewer(it, currentVersionCode(app)) } }
            if (result.isSuccess) prefs.lastUpdateCheckMs = System.currentTimeMillis()
            main.post { callback(result) }
        }
    }

    /** Vérification en arrière-plan : une notification par nouvelle version. */
    fun backgroundCheck(ctx: Context, done: () -> Unit = {}) {
        val app = ctx.applicationContext
        check(app) { r ->
            val prefs = Prefs(app)
            r.getOrNull()?.takeIf { it.versionCode > prefs.notifiedUpdate }?.let {
                prefs.notifiedUpdate = it.versionCode
                prefs.log("Nouvelle version disponible : ${it.name}")
                Notifier.update(app, it)
            }
            done()
        }
    }

    /** Vérification automatique deux fois par jour, même si l'app n'est pas ouverte. */
    fun scheduleDaily(ctx: Context) {
        if (TestTools.enabled) return
        val app = ctx.applicationContext
        val pi = PendingIntent.getBroadcast(
            app, 2,
            Intent(app, RegisterReceiver::class.java).setAction(ACTION_CHECK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        app.getSystemService(AlarmManager::class.java).setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
            AlarmManager.INTERVAL_HALF_DAY,
            pi
        )
    }
}
