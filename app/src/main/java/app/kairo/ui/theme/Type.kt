package app.kairo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import app.kairo.R

private fun flex(weight: FontWeight) = Font(
    resId = R.font.google_sans_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("ROND", 50f),
    ),
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

private val base = Typography(fontFamily = GoogleSansFlex)

val KairoTypography = Typography(
    displayLarge = base.displayLarge.tnum(), displayMedium = base.displayMedium.tnum(), displaySmall = base.displaySmall.tnum(),
    headlineLarge = base.headlineLarge.tnum(), headlineMedium = base.headlineMedium.tnum(), headlineSmall = base.headlineSmall.tnum(),
    titleLarge = base.titleLarge.tnum(), titleMedium = base.titleMedium.tnum(), titleSmall = base.titleSmall.tnum(),
    bodyLarge = base.bodyLarge.tnum(), bodyMedium = base.bodyMedium.tnum(), bodySmall = base.bodySmall.tnum(),
    labelLarge = base.labelLarge.tnum(), labelMedium = base.labelMedium.tnum(), labelSmall = base.labelSmall.tnum(),
    displayLargeEmphasized = base.displayLargeEmphasized.tnum(), displayMediumEmphasized = base.displayMediumEmphasized.tnum(),
    displaySmallEmphasized = base.displaySmallEmphasized.tnum(), headlineLargeEmphasized = base.headlineLargeEmphasized.tnum(),
    headlineMediumEmphasized = base.headlineMediumEmphasized.tnum(), headlineSmallEmphasized = base.headlineSmallEmphasized.tnum(),
    titleLargeEmphasized = base.titleLargeEmphasized.tnum(), titleMediumEmphasized = base.titleMediumEmphasized.tnum(),
    titleSmallEmphasized = base.titleSmallEmphasized.tnum(), bodyLargeEmphasized = base.bodyLargeEmphasized.tnum(),
    bodyMediumEmphasized = base.bodyMediumEmphasized.tnum(), bodySmallEmphasized = base.bodySmallEmphasized.tnum(),
    labelLargeEmphasized = base.labelLargeEmphasized.tnum(), labelMediumEmphasized = base.labelMediumEmphasized.tnum(),
    labelSmallEmphasized = base.labelSmallEmphasized.tnum(),
)
