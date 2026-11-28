package app.kairo.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import app.kairo.data.settings.AppSettings
import app.kairo.data.settings.Seed

/**
 * Builds the color scheme from settings: wallpaper (dynamic) colors or a seed, at a contrast level.
 * Whatever the source, surfaces are replaced with neutral grays (rule 3).
 */
fun kairoScheme(context: Context, s: AppSettings, dark: Boolean): ColorScheme {
    val base = if (dark) KairoDarkColors else KairoLightColors
    val accents: ColorScheme = if (s.wallpaperColor) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (s.seed == Seed.Default) {
        // v2 default: the Google blue/green palette from the canvas (one contrast level for now).
        base
    } else {
        val v = SeedSchemes.data.getValue(s.seed.name)[if (dark) 1 else 0][s.contrast.ordinal]
        val r = SeedSchemes.ROLES.mapIndexed { i, name -> name to Color(v[i]) }.toMap()
        base.copy(
            primary = r.getValue("primary"), onPrimary = r.getValue("onPrimary"),
            primaryContainer = r.getValue("primaryContainer"), onPrimaryContainer = r.getValue("onPrimaryContainer"),
            inversePrimary = r.getValue("inversePrimary"),
            secondary = r.getValue("secondary"), onSecondary = r.getValue("onSecondary"),
            secondaryContainer = r.getValue("secondaryContainer"), onSecondaryContainer = r.getValue("onSecondaryContainer"),
            tertiary = r.getValue("tertiary"), onTertiary = r.getValue("onTertiary"),
            tertiaryContainer = r.getValue("tertiaryContainer"), onTertiaryContainer = r.getValue("onTertiaryContainer"),
            error = r.getValue("error"), onError = r.getValue("onError"),
            errorContainer = r.getValue("errorContainer"), onErrorContainer = r.getValue("onErrorContainer"),
            outline = r.getValue("outline"), outlineVariant = r.getValue("outlineVariant"),
            onSurface = r.getValue("onSurface"), onBackground = r.getValue("onSurface"), onSurfaceVariant = r.getValue("onSurfaceVariant"),
            surfaceTint = r.getValue("primary"),
            primaryFixed = r.getValue("primaryFixed"), primaryFixedDim = r.getValue("primaryFixedDim"),
            onPrimaryFixed = r.getValue("onPrimaryFixed"), onPrimaryFixedVariant = r.getValue("onPrimaryFixedVariant"),
            secondaryFixed = r.getValue("secondaryFixed"), secondaryFixedDim = r.getValue("secondaryFixedDim"),
            onSecondaryFixed = r.getValue("onSecondaryFixed"), onSecondaryFixedVariant = r.getValue("onSecondaryFixedVariant"),
            tertiaryFixed = r.getValue("tertiaryFixed"), tertiaryFixedDim = r.getValue("tertiaryFixedDim"),
            onTertiaryFixed = r.getValue("onTertiaryFixed"), onTertiaryFixedVariant = r.getValue("onTertiaryFixedVariant"),
        )
    }
    // Neutral gray surfaces, always.
    return accents.copy(
        background = base.background, surface = base.surface, surfaceDim = base.surfaceDim, surfaceBright = base.surfaceBright,
        surfaceContainerLowest = base.surfaceContainerLowest, surfaceContainerLow = base.surfaceContainerLow,
        surfaceContainer = base.surfaceContainer, surfaceContainerHigh = base.surfaceContainerHigh,
        surfaceContainerHighest = base.surfaceContainerHighest, surfaceVariant = base.surfaceVariant,
        inverseSurface = base.inverseSurface, inverseOnSurface = base.inverseOnSurface, scrim = base.scrim,
    )
}
