package com.metrolist.android.ui.nectar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NectarLightColorScheme = lightColorScheme(
    primary = Color(0xFF006B56),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8EF7DD),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF345B54),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2E9DE),
    onSecondaryContainer = Color(0xFF07201B),
    tertiary = Color(0xFF006C9C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCE8FF),
    onTertiaryContainer = Color(0xFF001E2E),
    background = Color(0xFFF3F8F5),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF3F8F5),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFE1E9E4),
    onSurfaceVariant = Color(0xFF404944),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF3EF),
    surfaceContainer = Color(0xFFE8EFEB),
    surfaceContainerHigh = Color(0xFFE2EAE5),
    surfaceContainerHighest = Color(0xFFDCE5DF),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFC0C9C3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

private val NectarDarkColorScheme = darkColorScheme(
    primary = Color(0xFF72DAC1),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF00513F),
    onPrimaryContainer = Color(0xFF8EF7DD),
    secondary = Color(0xFFA6D0C4),
    onSecondary = Color(0xFF10372F),
    secondaryContainer = Color(0xFF294F47),
    onSecondaryContainer = Color(0xFFC2E9DE),
    tertiary = Color(0xFF80CFFF),
    onTertiary = Color(0xFF00344F),
    tertiaryContainer = Color(0xFF004B70),
    onTertiaryContainer = Color(0xFFBCE8FF),
    background = Color(0xFF101512),
    onBackground = Color(0xFFE0E7E1),
    surface = Color(0xFF101512),
    onSurface = Color(0xFFE0E7E1),
    surfaceVariant = Color(0xFF202823),
    onSurfaceVariant = Color(0xFFC0C9C3),
    surfaceContainerLowest = Color(0xFF0B100D),
    surfaceContainerLow = Color(0xFF181E1A),
    surfaceContainer = Color(0xFF1C231E),
    surfaceContainerHigh = Color(0xFF272E29),
    surfaceContainerHighest = Color(0xFF323A34),
    outline = Color(0xFF8A938D),
    outlineVariant = Color(0xFF404944),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

val NectarTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

val NectarShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun NectarTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val motionScheme = MotionScheme.expressive()
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) NectarDarkColorScheme else NectarLightColorScheme,
        typography = NectarTypography,
        shapes = NectarShapes,
        motionScheme = motionScheme,
        content = content,
    )
}
