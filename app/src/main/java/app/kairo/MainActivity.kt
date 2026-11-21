package app.kairo

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import app.kairo.data.settings.AppSettings
import app.kairo.lock.AppLock
import app.kairo.lock.LockScreen
import android.os.Build
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingAction = intent.getStringExtra(EXTRA_ACTION)
        setContent {
            val app = application as KairoApplication
            val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent // first frame waits for settings (fast), avoids a theme flash
            current = s
            // Cold start with the lock on: covered before the first frame.
            if (!coldStartChecked) { coldStartChecked = true; if (s.appLock) AppLock.locked = true }
            // Hide app content from the recents screenshot while the lock is on (Android 13+).
            LaunchedEffect(s.appLock) { if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!s.appLock) }
            val dark = when (s.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val context = LocalContext.current
            KairoTheme(darkTheme = dark, colorScheme = remember(s, dark) { kairoScheme(context, s, dark) }) {
                Box {
                    KairoApp(action = pendingAction, onActionHandled = { pendingAction = null })
                    // Ask as soon as the lock shows (only while we're in front).
                    LaunchedEffect(AppLock.locked) { if (AppLock.locked && s.appLock) AppLock.unlock(this@MainActivity) }
                    LockScreen(AppLock.locked && s.appLock) { AppLock.unlock(this@MainActivity) }
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction = intent.getStringExtra(EXTRA_ACTION)
    }

    companion object {
        const val EXTRA_ACTION = "kairo.action"
        const val ACTION_QUICK_ADD = "quick_add"
        const val ACTION_ADD_EXPENSE = "add_expense"
        const val ACTION_FOCUS = "focus"
    }
}
