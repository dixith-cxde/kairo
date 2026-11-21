package app.kairo.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun KairoTheme(
    darkTheme: Boolean = true, // Kairo rule 1: dark is the default. Settings (step 12) will let users pick.
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKairoSurfaces provides if (darkTheme) KairoDarkSurfaces else KairoLightSurfaces,
    ) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) KairoDarkColors else KairoLightColors,
            motionScheme = MotionScheme.expressive(),
            typography = KairoTypography,
            content = content,
        )
    }
}

/** Kairo-only tokens, read like MaterialTheme: `KairoTheme.surfaces.navBg`. */
object KairoTheme {
    val surfaces: KairoSurfaces
        @Composable @ReadOnlyComposable
        get() = LocalKairoSurfaces.current
}

private val LocalKairoSurfaces = staticCompositionLocalOf { KairoDarkSurfaces }
