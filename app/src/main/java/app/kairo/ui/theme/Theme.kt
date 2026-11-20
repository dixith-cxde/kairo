package app.kairo.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

@Composable
fun KairoTheme(
    darkTheme: Boolean = true, // Kairo rule 1: dark is the default. Settings (step 12) will let users pick.
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) KairoDarkColors else KairoLightColors,
        motionScheme = MotionScheme.expressive(),
        typography = KairoTypography,
        content = content,
    )
}
