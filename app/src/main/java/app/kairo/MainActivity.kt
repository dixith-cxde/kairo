package app.kairo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.kairo.ui.theme.KairoTheme
import app.kairo.ui.theme.kairoScheme
import app.kairo.data.settings.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                KairoApp()
            }
        }
    }
}
