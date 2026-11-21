package app.kairo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import app.kairo.R

private fun flex(weight: FontWeight, round: Float = 50f) = Font(
    resId = R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("ROND", round),
    ),
)

/**
 * Display / headline / large titles: fully rounded (ROND 100), the friendlier "Expressive" face
 * Google uses for big type. Body text stays at ROND 50 for reading.
 */
val GoogleSansFlexRounded = FontFamily(
    flex(FontWeight.Normal, 100f), flex(FontWeight.Medium, 100f), flex(FontWeight.SemiBold, 100f),
    flex(FontWeight.Bold, 100f), flex(FontWeight.ExtraBold, 100f),
)

val GoogleSansFlex = FontFamily(
    flex(FontWeight.Normal),   // 400
    flex(FontWeight.Medium),   // 500
    flex(FontWeight.SemiBold), // 600
    flex(FontWeight.Bold),     // 700
    flex(FontWeight.ExtraBold),// 800 (hero numbers, later)
)

/** Rule 8: tabular digits everywhere ("tnum" only changes digits, so it's safe on all text). */
private fun TextStyle.tnum() = copy(fontFeatureSettings = "tnum")
/** Big type: rounded face. */
private fun TextStyle.big() = copy(fontFamily = GoogleSansFlexRounded, fontFeatureSettings = "tnum")

private val base = Typography(fontFamily = GoogleSansFlex)

val KairoTypography = Typography(
    displayLarge = base.displayLarge.big(), displayMedium = base.displayMedium.big(), displaySmall = base.displaySmall.big().copy(fontWeight = FontWeight.SemiBold) /* screen titles (rule: displaySmall emphasized); the flexible app bar reads this role */,
    headlineLarge = base.headlineLarge.big(), headlineMedium = base.headlineMedium.big(), headlineSmall = base.headlineSmall.big(),
    titleLarge = base.titleLarge.big(), titleMedium = base.titleMedium.tnum(), titleSmall = base.titleSmall.tnum(),
    bodyLarge = base.bodyLarge.tnum(), bodyMedium = base.bodyMedium.tnum(), bodySmall = base.bodySmall.tnum(),
    labelLarge = base.labelLarge.tnum(), labelMedium = base.labelMedium.tnum(), labelSmall = base.labelSmall.tnum(),
    displayLargeEmphasized = base.displayLargeEmphasized.big(), displayMediumEmphasized = base.displayMediumEmphasized.big(),
    displaySmallEmphasized = base.displaySmallEmphasized.big(), headlineLargeEmphasized = base.headlineLargeEmphasized.big(),
    headlineMediumEmphasized = base.headlineMediumEmphasized.big(), headlineSmallEmphasized = base.headlineSmallEmphasized.big(),
    titleLargeEmphasized = base.titleLargeEmphasized.big(), titleMediumEmphasized = base.titleMediumEmphasized.tnum(),
    titleSmallEmphasized = base.titleSmallEmphasized.tnum(), bodyLargeEmphasized = base.bodyLargeEmphasized.tnum(),
    bodyMediumEmphasized = base.bodyMediumEmphasized.tnum(), bodySmallEmphasized = base.bodySmallEmphasized.tnum(),
    labelLargeEmphasized = base.labelLargeEmphasized.tnum(), labelMediumEmphasized = base.labelMediumEmphasized.tnum(),
    labelSmallEmphasized = base.labelSmallEmphasized.tnum(),
)
