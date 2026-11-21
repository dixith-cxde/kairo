package app.kairo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {
    /** Set by widgets ("open quick add", "add expense"); consumed once by KairoApp. */
    private var pendingAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingAction = intent.getStringExtra(EXTRA_ACTION)
        setContent {
            val app = application as KairoApplication
            val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val s = settings ?: return@setContent // first frame waits for settings (fast), avoids a theme flash
            val dark = when (s.theme) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            val context = LocalContext.current
            KairoTheme(darkTheme = dark, colorScheme = remember(s, dark) { kairoScheme(context, s, dark) }) {
                KairoApp(action = pendingAction, onActionHandled = { pendingAction = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingAction = intent.getStringExtra(EXTRA_ACTION)
    }

    companion object {
        const val EXTRA_ACTION = "kairo.action"
        const val ACTION_QUICK_ADD = "quick_add"
        const val ACTION_ADD_EXPENSE = "add_expense"
    }
}
