package xyz.ksharma.huezoo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Space Grotesk fonts should be placed in:
// composeApp/src/commonMain/composeResources/font/
// Files: SpaceGrotesk-Regular.ttf, SpaceGrotesk-Medium.ttf,
//        SpaceGrotesk-SemiBold.ttf, SpaceGrotesk-Bold.ttf
// Download from: https://fonts.google.com/specimen/Space+Grotesk
//
// Once fonts are added, replace FontFamily.SansSerif below with:
// FontFamily(
//     Font(Res.font.SpaceGrotesk_Regular, FontWeight.Normal),
//     Font(Res.font.SpaceGrotesk_Medium, FontWeight.Medium),
//     Font(Res.font.SpaceGrotesk_SemiBold, FontWeight.SemiBold),
//     Font(Res.font.SpaceGrotesk_Bold, FontWeight.Bold),
// )

@Composable
fun huezooTypography(): Typography {
    val spaceGrotesk = FontFamily.SansSerif
    return Typography(
        displayLarge = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = spaceGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
        ),
    )
}
