package app.kairo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// v2 default palette (owner, 2026-11-28): Google blue primary/secondary, Google green tertiary.
// Values come from the v2 canvas tokens (.k.dark / .k.light in Main.dc.html). Roles the canvas
// doesn't use (onSecondary, onTertiary, onError, outline, the Fixed roles) are filled from the same
// Google blue/green tonal palettes; they are not on the canvas, so treat them as unconfirmed.
// Surfaces: neutral gray, chroma 0 (rule 3).

val KairoDarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0), onPrimaryContainer = Color(0xFFD3E3FD),
    inversePrimary = Color(0xFF0B57D0),
    secondary = Color(0xFF7FCFFF), onSecondary = Color(0xFF003355),
    secondaryContainer = Color(0xFF004A77), onSecondaryContainer = Color(0xFFC2E7FF),
    tertiary = Color(0xFF6DD58C), onTertiary = Color(0xFF0A3818),
    tertiaryContainer = Color(0xFF0F5223), onTertiaryContainer = Color(0xFFC4EED0),
    error = Color(0xFFFF9F99), onError = Color(0xFF690005),
    errorContainer = Color(0xFFD7383B), onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF8E8E8E), outlineVariant = Color(0xFF474747),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFFC6C6C6),
    surfaceTint = Color(0xFFA8C7FA),
    scrim = Color(0xFF000000),
    // Neutral grays
    background = Color(0xFF0E0E0E),              // bg
    surface = Color(0xFF131313),
    surfaceDim = Color(0xFF131313),
    surfaceBright = Color(0xFF393939),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1B1B1B),
    surfaceContainer = Color(0xFF1F1F1F),        // item
    surfaceContainerHigh = Color(0xFF2A2A2A),    // nav, chips
    surfaceContainerHighest = Color(0xFF353535),
    surfaceVariant = Color(0xFF353535),
    inverseSurface = Color(0xFFE2E2E2),
    inverseOnSurface = Color(0xFF1B1B1B),
    primaryFixed = Color(0xFFD3E3FD), primaryFixedDim = Color(0xFFA8C7FA),
    onPrimaryFixed = Color(0xFF041E49), onPrimaryFixedVariant = Color(0xFF0842A0),
    secondaryFixed = Color(0xFFC2E7FF), secondaryFixedDim = Color(0xFF7FCFFF),
    onSecondaryFixed = Color(0xFF001D35), onSecondaryFixedVariant = Color(0xFF004A77),
    tertiaryFixed = Color(0xFFC4EED0), tertiaryFixedDim = Color(0xFF6DD58C),
    onTertiaryFixed = Color(0xFF072711), onTertiaryFixedVariant = Color(0xFF0F5223),
)

val KairoLightColors = lightColorScheme(
    primary = Color(0xFF0B57D0), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
    inversePrimary = Color(0xFFA8C7FA),
    secondary = Color(0xFF00639B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2E7FF), onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF146C2E), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EED0), onTertiaryContainer = Color(0xFF072711),
    error = Color(0xFFB31B25), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFD7383B), onErrorContainer = Color(0xFFFFFFFF),
    outline = Color(0xFF747775), outlineVariant = Color(0xFFC6C6C6),
    onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF1B1B1B), onSurfaceVariant = Color(0xFF474747),
    surfaceTint = Color(0xFF0B57D0),
    scrim = Color(0xFF000000),
    // Neutral grays
    background = Color(0xFFF1F1F1),              // bg
    surface = Color(0xFFF9F9F9),
    surfaceDim = Color(0xFFDADADA),
    surfaceBright = Color(0xFFF9F9F9),
    surfaceContainerLowest = Color(0xFFFFFFFF),  // item, nav
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2), // chips
    surfaceVariant = Color(0xFFE2E2E2),
    inverseSurface = Color(0xFF303030),
    inverseOnSurface = Color(0xFFF1F1F1),
    primaryFixed = Color(0xFFD3E3FD), primaryFixedDim = Color(0xFFA8C7FA),
    onPrimaryFixed = Color(0xFF041E49), onPrimaryFixedVariant = Color(0xFF0842A0),
    secondaryFixed = Color(0xFFC2E7FF), secondaryFixedDim = Color(0xFF7FCFFF),
    onSecondaryFixed = Color(0xFF001D35), onSecondaryFixedVariant = Color(0xFF004A77),
    tertiaryFixed = Color(0xFFC4EED0), tertiaryFixedDim = Color(0xFF6DD58C),
    onTertiaryFixed = Color(0xFF072711), onTertiaryFixedVariant = Color(0xFF0F5223),
)
