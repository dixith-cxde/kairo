package app.kairo

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import app.kairo.data.settings.AppSettings
import app.kairo.lock.AppLock
import app.kairo.lock.LockScreen
import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.kairo.data.settings.ThemeMode
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.theme.kairoScheme

/** FragmentActivity (a ComponentActivity) because the system BiometricPrompt needs it. */
class MainActivity : FragmentActivity() {
    /** Set by widgets ("open quick add", "add expense"); consumed once by KairoApp. */
    private var pendingAction by mutableStateOf<String?>(null)
    /** Latest settings, for the lifecycle callbacks below. */
    private var current: AppSettings? = null
    private var coldStartChecked = false

    /** Quick add widget's mic: the system speech recognizer, then quick add with the words filled in. */
    private val speech = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val words = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        pendingAction = if (words.isBlank()) ACTION_QUICK_ADD else "$ACTION_QUICK_ADD:$words"
    }

    private fun handle(action: String?) {
        if (action != ACTION_VOICE_ADD) { pendingAction = action; return }
        val ask = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Add a task")
        runCatching { speech.launch(ask) }.onFailure { pendingAction = ACTION_QUICK_ADD } // no recognizer: just type
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent.getStringExtra(EXTRA_ACTION))
        setContent {
            val app = application as KairoApplication
            val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent // first frame waits for settings (fast), avoids a theme flash
            current = s
            // Cold start with the lock on: covered before the first frame.
            if (!coldStartChecked) { coldStartChecked = true; if (s.appLock) AppLock.locked = true }
            // Hide app content from the recents screenshot while the lock is on (Android 13+).
            // "Block screenshots" (or the lock on Android 12, which has no recents-only switch) uses
            // FLAG_SECURE: no screenshots, screen recordings, casting or recents preview.
            LaunchedEffect(s.appLock, s.blockScreenshots) {
                if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!s.appLock)
                val secure = s.blockScreenshots || (s.appLock && Build.VERSION.SDK_INT < 33)
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val dark = when (s.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val context = LocalContext.current
            KairoTheme(darkTheme = dark, colorScheme = remember(s, dark) { kairoScheme(context, s, dark) }) {
                val locked = AppLock.locked && s.appLock
                Box {
                    // While locked, the app underneath is hidden from TalkBack and other
                    // accessibility readers too, not just covered visually.
                    // Nothing of the app is composed while locked. Sheets and menus are separate
                    // windows that would draw above the lock screen, so a widget's "add" could be
                    // used without unlocking (auth bypass). The widget's action waits in
                    // pendingAction and runs once unlocked.
                    if (!locked) KairoApp(action = pendingAction, onActionHandled = { pendingAction = null })
                    // Ask as soon as the lock shows (only while we're in front).
                    LaunchedEffect(AppLock.locked) { if (AppLock.locked && s.appLock) AppLock.unlock(this@MainActivity) }
                    LockScreen(locked) { AppLock.unlock(this@MainActivity) }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AppLock.onBackground()
    }

    override fun onStart() {
        super.onStart()
        current?.let { AppLock.onForeground(it.appLock, it.lockAfterSec) }
        // Every open: ask GitHub for a newer release (cheap; see Updater).
        val app = application as KairoApplication
        app.updater.checkOnOpen(app.appScope)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent.getStringExtra(EXTRA_ACTION))
    }

    companion object {
        const val EXTRA_ACTION = "kairo.action"
        const val ACTION_QUICK_ADD = "quick_add"
        const val ACTION_ADD_EXPENSE = "add_expense"
        const val ACTION_FOCUS = "focus"
        const val ACTION_VOICE_ADD = "voice_add"
        const val ACTION_STATS = "stats"
        const val ACTION_ADD_INCOME = "add_income"
        const val ACTION_ADD_TRANSFER = "add_transfer"
        /** "task:12" opens that task. */
        fun taskAction(id: Long) = "task:$id"
    }
}
