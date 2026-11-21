package app.kairo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Kairo's named neutral surfaces (docs/03-design-system.md, canvas m3e.css).
 * They don't map to the same M3 role in light and dark (light item = white = surfaceContainerLowest,
 * dark item = surfaceContainer), so screens read these names instead of M3 roles.
 */
@Immutable
data class KairoSurfaces(
    val bg: Color,        // screen background
    val item: Color,      // grouped containers, cards, rows
    val itemHigh: Color,  // nested / raised items
    val navBg: Color,     // floating nav, toolbars
    val floatShadow: Color, // shadow under floating nav, FAB, toolbars
)

val KairoDarkSurfaces = KairoSurfaces(
    bg = Color(0xFF0E0E0E),
    item = Color(0xFF1F1F1F),
    itemHigh = Color(0xFF2A2A2A),
    navBg = Color(0xFF2A2A2A),
    floatShadow = Color(0x99000000),   // rgba(0,0,0,.6)
)

val KairoLightSurfaces = KairoSurfaces(
    bg = Color(0xFFEEEEEE),
    item = Color(0xFFFFFFFF),
    itemHigh = Color(0xFFF3F3F3),
    navBg = Color(0xFFFFFFFF),
    floatShadow = Color(0x470A0D28),   // rgba(10,13,40,.28)
)
