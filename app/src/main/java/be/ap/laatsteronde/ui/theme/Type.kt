package be.ap.laatsteronde.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.R

// Fonts from the Figma prototype (both SIL Open Font License). Each file is a variable font,
// so one file gives us every weight. Before Android 8 (API 26) the weight setting is ignored.
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Fredoka: round letters like the logo, for titles and headings. */
val Fredoka = FontFamily(
    variableFont(R.font.fredoka, FontWeight.SemiBold),
    variableFont(R.font.fredoka, FontWeight.Bold)
)

/** Inter: calm and readable, for normal text, labels and buttons. */
val Inter = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold)
)

val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = Fredoka, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
)
