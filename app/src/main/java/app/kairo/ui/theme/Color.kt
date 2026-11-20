package app.kairo.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Accents: SchemeTonalSpot (2021) from seed #79D0FD (same as Stash).
// Surfaces: neutral gray, chroma 0. Comment = M3 tone.

val KairoDarkColors = darkColorScheme(
    primary = Color(0xFF8ECFF2), onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D68), onPrimaryContainer = Color(0xFFC2E8FF),
    inversePrimary = Color(0xFF1A6585),
    secondary = Color(0xFFB5C9D7), onSecondary = Color(0xFF20333D),
    secondaryContainer = Color(0xFF364954), onSecondaryContainer = Color(0xFFD1E5F3),
    tertiary = Color(0xFFC9C1EA), onTertiary = Color(0xFF312C4C),
    tertiaryContainer = Color(0xFF484264), onTertiaryContainer = Color(0xFFE5DEFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A9297), outlineVariant = Color(0xFF41484D),
    onBackground = Color(0xFFDFE3E7),
    onSurface = Color(0xFFDFE3E7), onSurfaceVariant = Color(0xFFC0C7CD),
    surfaceTint = Color(0xFF8ECFF2),
    scrim = Color(0xFF000000),
    // Neutral grays
    background = Color(0xFF0E0E0E),              // T4  · bg
    surface = Color(0xFF131313),                 // T6
    surfaceDim = Color(0xFF131313),              // T6
    surfaceBright = Color(0xFF393939),           // T24
    surfaceContainerLowest = Color(0xFF0E0E0E),  // T4
    surfaceContainerLow = Color(0xFF1B1B1B),     // T10
    surfaceContainer = Color(0xFF1F1F1F),        // T12 · item
    surfaceContainerHigh = Color(0xFF2A2A2A),    // T17 · itemHigh, navBg
    surfaceContainerHighest = Color(0xFF353535), // T22
    surfaceVariant = Color(0xFF353535),          // T22
    inverseSurface = Color(0xFFE2E2E2),          // T90
    inverseOnSurface = Color(0xFF303030),        // T20
    // Fixed roles (same in light and dark)
    primaryFixed = Color(0xFFC2E8FF), primaryFixedDim = Color(0xFF8ECFF2),
    onPrimaryFixed = Color(0xFF001E2C), onPrimaryFixedVariant = Color(0xFF004D68),
    secondaryFixed = Color(0xFFD1E5F3), secondaryFixedDim = Color(0xFFB5C9D7),
    onSecondaryFixed = Color(0xFF091E28), onSecondaryFixedVariant = Color(0xFF364954),
    tertiaryFixed = Color(0xFFE5DEFF), tertiaryFixedDim = Color(0xFFC9C1EA),
    onTertiaryFixed = Color(0xFF1C1736), onTertiaryFixedVariant = Color(0xFF484264),
)

val KairoLightColors = lightColorScheme(
    primary = Color(0xFF1A6585), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E8FF), onPrimaryContainer = Color(0xFF004D68),
    inversePrimary = Color(0xFF8ECFF2),
    secondary = Color(0xFF4E616D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E5F3), onSecondaryContainer = Color(0xFF364954),
    tertiary = Color(0xFF605A7D), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5DEFF), onTertiaryContainer = Color(0xFF484264),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF71787D), outlineVariant = Color(0xFFC0C7CD),
    onBackground = Color(0xFF171C1F),
    onSurface = Color(0xFF171C1F), onSurfaceVariant = Color(0xFF41484D),
    surfaceTint = Color(0xFF1A6585),
    scrim = Color(0xFF000000),
    // Neutral grays
    background = Color(0xFFEEEEEE),              // T94 · bg
    surface = Color(0xFFF9F9F9),                 // T98
    surfaceDim = Color(0xFFDADADA),              // T87
    surfaceBright = Color(0xFFF9F9F9),           // T98
    surfaceContainerLowest = Color(0xFFFFFFFF),  // T100 · item, navBg
    surfaceContainerLow = Color(0xFFF3F3F3),     // T96 · itemHigh
    surfaceContainer = Color(0xFFEEEEEE),        // T94
    surfaceContainerHigh = Color(0xFFE8E8E8),    // T92
    surfaceContainerHighest = Color(0xFFE2E2E2), // T90
    surfaceVariant = Color(0xFFE2E2E2),          // T90
    inverseSurface = Color(0xFF303030),          // T20
    inverseOnSurface = Color(0xFFF1F1F1),        // T95
    // Fixed roles (same in light and dark)
    primaryFixed = Color(0xFFC2E8FF), primaryFixedDim = Color(0xFF8ECFF2),
    onPrimaryFixed = Color(0xFF001E2C), onPrimaryFixedVariant = Color(0xFF004D68),
    secondaryFixed = Color(0xFFD1E5F3), secondaryFixedDim = Color(0xFFB5C9D7),
    onSecondaryFixed = Color(0xFF091E28), onSecondaryFixedVariant = Color(0xFF364954),
    tertiaryFixed = Color(0xFFE5DEFF), tertiaryFixedDim = Color(0xFFC9C1EA),
    onTertiaryFixed = Color(0xFF1C1736), onTertiaryFixedVariant = Color(0xFF484264),
)
