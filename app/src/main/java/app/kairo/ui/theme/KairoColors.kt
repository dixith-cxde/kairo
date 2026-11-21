package app.kairo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.kairo.data.model.ListColor

/**
 * Kairo's own color tokens (docs/03-design-system.md, canvas m3e.css), next to MaterialTheme.colorScheme.
 * Neutral surfaces don't map to the same M3 role in light and dark (light item = white =
 * surfaceContainerLowest, dark item = surfaceContainer), so screens read these names.
 */
@Immutable
data class KairoColors(
    val bg: Color,          // screen background
    val item: Color,        // grouped containers, cards, rows
    val itemHigh: Color,    // nested / raised items
    val navBg: Color,       // floating nav, toolbars
    val floatShadow: Color, // shadow under floating nav, FAB, toolbars
    val prioHigh: Color,
    val prioMed: Color,
    val prioLow: Color,
    private val accents: Map<ListColor, Color>,     // list accent (dots, shapes)
    private val containers: Map<ListColor, Color>,  // list container (chips, list icons)
    private val onContainers: Map<ListColor, Color>,
) {
    fun accent(c: ListColor) = accents.getValue(c)
    fun container(c: ListColor) = containers.getValue(c)
    fun onContainer(c: ListColor) = onContainers.getValue(c)
}

private fun m(vararg p: Pair<ListColor, Long>) = p.associate { it.first to Color(it.second) }

val KairoDarkColorTokens = KairoColors(
    bg = Color(0xFF0E0E0E), item = Color(0xFF1F1F1F), itemHigh = Color(0xFF2A2A2A), navBg = Color(0xFF2A2A2A),
    floatShadow = Color(0x99000000),
    prioHigh = Color(0xFFFFB4A9), prioMed = Color(0xFFFEBB2A), prioLow = Color(0xFFB9C3FF),
    accents = m(
        ListColor.Indigo to 0xFFB9C3FF, ListColor.Coral to 0xFFFFB4A1, ListColor.Amber to 0xFFFEBB2A,
        ListColor.Green to 0xFF7EDB82, ListColor.Teal to 0xFF5DD9D8, ListColor.Pink to 0xFFFFB0CE,
        ListColor.Violet to 0xFFD7BAFF,
    ),
    containers = m(
        ListColor.Indigo to 0xFF0D35BB, ListColor.Coral to 0xFF891E00, ListColor.Amber to 0xFF5E4200,
        ListColor.Green to 0xFF005319, ListColor.Teal to 0xFF004F4F, ListColor.Pink to 0xFF8C0054,
        ListColor.Violet to 0xFF5D22A7,
    ),
    onContainers = m(
        ListColor.Indigo to 0xFFDEE1FF, ListColor.Coral to 0xFFFFDBD2, ListColor.Amber to 0xFFFFDEA8,
        ListColor.Green to 0xFF99F89C, ListColor.Teal to 0xFF7DF5F5, ListColor.Pink to 0xFFFFD9E5,
        ListColor.Violet to 0xFFEDDCFF,
    ),
)

val KairoLightColorTokens = KairoColors(
    bg = Color(0xFFEEEEEE), item = Color(0xFFFFFFFF), itemHigh = Color(0xFFF3F3F3), navBg = Color(0xFFFFFFFF),
    floatShadow = Color(0x470A0D28),
    prioHigh = Color(0xFFBB1713), prioMed = Color(0xFF7C5800), prioLow = Color(0xFF3351D3),
    accents = m(
        ListColor.Indigo to 0xFF3351D3, ListColor.Coral to 0xFFAB3514, ListColor.Amber to 0xFF7C5800,
        ListColor.Green to 0xFF006E24, ListColor.Teal to 0xFF006A6A, ListColor.Pink to 0xFFAE236D,
        ListColor.Violet to 0xFF753FC1,
    ),
    containers = m(
        ListColor.Indigo to 0xFFDEE1FF, ListColor.Coral to 0xFFFFDBD2, ListColor.Amber to 0xFFFFDEA8,
        ListColor.Green to 0xFF99F89C, ListColor.Teal to 0xFF7DF5F5, ListColor.Pink to 0xFFFFD9E5,
        ListColor.Violet to 0xFFEDDCFF,
    ),
    onContainers = m(
        ListColor.Indigo to 0xFF001258, ListColor.Coral to 0xFF3C0800, ListColor.Amber to 0xFF271900,
        ListColor.Green to 0xFF002106, ListColor.Teal to 0xFF002020, ListColor.Pink to 0xFF3E0022,
        ListColor.Violet to 0xFF280056,
    ),
)
