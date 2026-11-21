package app.kairo.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import app.kairo.KairoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A GitHub release that carries an APK. */
data class Release(val version: String, val notes: String, val apkUrl: String, val apkSize: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    /** [progress] 0..1, or null while the size is unknown. */
    data class Downloading(val release: Release, val progress: Float?) : UpdateState
    /** Handed to Android's installer; it shows its own confirm screen. */
    data class Installing(val release: Release) : UpdateState
    data class Failed(val release: Release?, val message: String) : UpdateState
}

/**
 * In-app updates from this repo's GitHub releases.
 *
 * Check: one small request to the "latest release" API, sent with the ETag from the last answer,
 * so an unchanged release comes back as "304 Not Modified" (no body, and GitHub doesn't count it
 * against the 60-an-hour limit). That makes checking on every open cheap.
 *
 * Install: the APK streams straight into a PackageInstaller session (no temp file), then Android
 * shows its own "Update Kairo?" screen. The APK must be signed with the same key as the installed
 * app, or Android refuses it.
 */
class Updater(private val app: KairoApplication) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Version the user said "Later" to; the sheet stays shut for it until they tap the Settings row. */
    val dismissed = MutableStateFlow<String?>(null)

    val currentVersion: String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    }.getOrNull() ?: "0"

    private val prefs = app.getSharedPreferences("update", Context.MODE_PRIVATE)
    private var lastCheck = 0L
    private var job: Job? = null

    /** Every time the app comes to the front. Skips if a check ran in the last minute (quick app switches). */
    fun checkOnOpen(scope: CoroutineScope) {
        if (SystemClock.elapsedRealtime() - lastCheck < 60_000 && lastCheck != 0L) return
        check(scope)
    }

    fun check(scope: CoroutineScope) {
        val s = _state.value
        if (s is UpdateState.Downloading || s is UpdateState.Installing || job?.isActive == true) return
        lastCheck = SystemClock.elapsedRealtime()
        job = scope.launch(Dispatchers.IO) {
            val prev = _state.value
            // An offered update stays on screen while we re-check; only an empty state shows "Checking".
            if (prev !is UpdateState.Available) _state.value = UpdateState.Checking
            _state.value = runCatching { fetchLatest() }.fold(
                onSuccess = { r -> if (r != null && newer(r.version, currentVersion)) UpdateState.Available(r) else UpdateState.UpToDate },
                // Offline or rate-limited on open: stay quiet, don't nag.
                onFailure = { if (prev is UpdateState.Available) prev else UpdateState.Idle },
            )
        }
    }

    private fun fetchLatest(): Release? {
        val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000; readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kairo-Android")
            prefs.getString("etag", null)?.let { setRequestProperty("If-None-Match", it) }
        }
        try {
            val body = when (conn.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> prefs.getString("body", null) ?: return null
                HttpURLConnection.HTTP_OK -> conn.inputStream.bufferedReader().use { it.readText() }.also { text ->
                    prefs.edit().putString("etag", conn.getHeaderField("ETag")).putString("body", text).apply()
                }
                HttpURLConnection.HTTP_NOT_FOUND -> return null // no releases yet
                else -> error("GitHub answered ${conn.responseCode}")
            }
            return parse(body)
        } finally { conn.disconnect() }
    }

    private fun parse(body: String): Release? {
        val json = JSONObject(body)
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null
        val assets = json.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) } ?: return null
        return Release(
            version = json.optString("tag_name").removePrefix("v").removePrefix("V"),
            notes = json.optString("body").trim(),
            apkUrl = apk.getString("browser_download_url"),
            apkSize = apk.optLong("size", -1),
        )
    }

    /** True when Android lets Kairo install APKs ("Install unknown apps" for Kairo). */
    fun canInstall() = app.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent() =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))

    fun downloadAndInstall(scope: CoroutineScope, release: Release) {
        if (_state.value is UpdateState.Downloading || _state.value is UpdateState.Installing) return
        job = scope.launch(Dispatchers.IO) {
            _state.value = UpdateState.Downloading(release, if (release.apkSize > 0) 0f else null)
            val installer = app.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(app.packageName)
                if (release.apkSize > 0) setSize(release.apkSize)
                // Lets Android skip its confirm screen once Kairo is the app's installer of record.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                if (Build.VERSION.SDK_INT >= 34) setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
            }
            val id = runCatching { installer.createSession(params) }.getOrElse {
                _state.value = UpdateState.Failed(release, "Couldn't start the install"); return@launch
            }
            val session = installer.openSession(id)
            try {
                val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 30_000
                    setRequestProperty("User-Agent", "Kairo-Android")
                }
                try {
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) error("Download failed (${conn.responseCode})")
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
                    conn.inputStream.use { input ->
                        session.openWrite("base.apk", 0, total).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var done = 0L
                            var shown = -1
                            while (true) {
                                ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total > 0) {
                                    // Only publish whole-percent steps: ~100 UI updates, not thousands.
                                    val pct = (done * 100 / total).toInt()
                                    if (pct != shown) {
                                        shown = pct
                                        session.setStagingProgress(done.toFloat() / total)
                                        _state.value = UpdateState.Downloading(release, done.toFloat() / total)
                                    }
                                }
                            }
                            session.fsync(out)
                        }
                    }
                } finally { conn.disconnect() }
                val pending = PendingIntent.getBroadcast(
                    app, id, Intent(app, InstallReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                _state.value = UpdateState.Installing(release)
                withContext(Dispatchers.Main) { session.commit(pending.intentSender) }
                session.close()
            } catch (e: Throwable) {
                runCatching { session.abandon() }
                _state.value = UpdateState.Failed(release, e.message ?: "Download failed")
            }
        }
    }

    internal fun onInstallResult(status: Int, message: String?) {
        val release = (_state.value as? UpdateState.Installing)?.release
        _state.value = when (status) {
            PackageInstaller.STATUS_SUCCESS -> UpdateState.UpToDate
            PackageInstaller.STATUS_FAILURE_ABORTED -> release?.let { UpdateState.Available(it) } ?: UpdateState.Idle
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                UpdateState.Failed(release, "This copy of Kairo is signed with a different key (e.g. a debug build). " +
                    "Uninstall it and install the release once; after that, updates work from here.")
            PackageInstaller.STATUS_FAILURE_STORAGE -> UpdateState.Failed(release, "Not enough storage")
            else -> UpdateState.Failed(release, message ?: "Install failed")
        }
    }

    companion object {
        const val REPO = "dixith-cxde/kairo"

        /** "1.2.10" > "1.2.9"; missing parts count as 0; non-digits are ignored ("1.2-beta" = 1.2). */
        fun newer(a: String, b: String): Boolean {
            fun parts(v: String) = v.split('.').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
            val x = parts(a); val y = parts(b)
            for (i in 0 until maxOf(x.size, y.size)) {
                val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
                if (d != 0) return d > 0
            }
            return false
        }
    }
}

/** Android's installer reports back here: show its confirm screen, or pass on the result. */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        (context.applicationContext as KairoApplication).updater
            .onInstallResult(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
    }
}
