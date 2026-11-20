package app.kairo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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

val KairoTypography = Typography(fontFamily = GoogleSansFlex)
