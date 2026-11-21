package app.kairo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun KairoTheme(
    darkTheme: Boolean = true, // Kairo rule 1: dark is the default; Settings lets users pick.
    colorScheme: ColorScheme? = null, // from Settings (seed / wallpaper / contrast); null = default
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKairoColors provides if (darkTheme) KairoDarkColorTokens else KairoLightColorTokens,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme ?: if (darkTheme) KairoDarkColors else KairoLightColors,
            motionScheme = MotionScheme.expressive(),
            typography = KairoTypography,
            content = content,
        )
    }
}

/** Kairo-only tokens, read like MaterialTheme: `KairoTheme.colors.navBg`. */
object KairoTheme {
    val colors: KairoColors
        @Composable @ReadOnlyComposable
        get() = LocalKairoColors.current
}

private val LocalKairoColors = staticCompositionLocalOf { KairoDarkColorTokens }
