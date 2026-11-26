package com.metrolist.android.ui.nectar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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

val BrinkEpisodeColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF201B0D),
    onPrimary = Color(0xFFFBE99C),
    primaryContainer = Color(0xFFA2E6F4),
    onPrimaryContainer = Color(0xFF0A242B),
    secondary = Color(0xFF4B4223),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB3EAF5),
    onSecondaryContainer = Color(0xFF102A32),
    tertiary = Color(0xFF67CEE6),
    onTertiary = Color(0xFF062129),
    tertiaryContainer = Color(0xFFEBD37A),
    onTertiaryContainer = Color(0xFF201B0D),
    background = Color(0xFFEBD37A),
    onBackground = Color(0xFF201B0D),
    surface = Color(0xFFEBD37A),
    onSurface = Color(0xFF201B0D),
    surfaceVariant = Color(0xFFDCC366),
    onSurfaceVariant = Color(0xFF4C4222),
    surfaceContainerLowest = Color(0xFFF6E49D),
    surfaceContainerLow = Color(0xFFEED885),
    surfaceContainer = Color(0xFFE5CA6D),
    surfaceContainerHigh = Color(0xFFB3EAF5),
    surfaceContainerHighest = Color(0xFFA2E6F4),
    outline = Color(0xFF7B6F46),
    outlineVariant = Color(0xFFCBB45B),
)

val BrinkPlayerColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF0A2A40),
    onPrimary = Color(0xFFF2F6F8),
    primaryContainer = Color(0xFF294352),
    onPrimaryContainer = Color(0xFFE4EEF3),
    secondary = Color(0xFFCBD7DE),
    onSecondary = Color(0xFF142733),
    secondaryContainer = Color(0xFFD6E1E7),
    onSecondaryContainer = Color(0xFF132632),
    tertiary = Color(0xFF152936),
    onTertiary = Color(0xFFE9F1F5),
    tertiaryContainer = Color(0xFF253C4A),
    onTertiaryContainer = Color(0xFFE2ECF1),
    background = Color(0xFF162936),
    onBackground = Color(0xFF10202B),
    surface = Color(0xFF8AA2B0),
    onSurface = Color(0xFF10202B),
    surfaceVariant = Color(0xFF5D7888),
    onSurfaceVariant = Color(0xFF243845),
    surfaceContainerLowest = Color(0xFF12232E),
    surfaceContainerLow = Color(0xFF1E3442),
    surfaceContainer = Color(0xFF294352),
    surfaceContainerHigh = Color(0xFFD6E1E7),
    surfaceContainerHighest = Color(0xFFE3ECF0),
    outline = Color(0xFF516B7A),
    outlineVariant = Color(0xFF6B8494),
)

val BrinkShowColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF092314),
    onPrimary = Color(0xFFB4F8C9),
    primaryContainer = Color(0xFF8EF4AE),
    onPrimaryContainer = Color(0xFF072312),
    secondary = Color(0xFF124626),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF7BE89E),
    onSecondaryContainer = Color(0xFF092816),
    tertiary = Color(0xFF2DB2E8),
    onTertiary = Color(0xFF042230),
    tertiaryContainer = Color(0xFF56C5F0),
    onTertiaryContainer = Color(0xFF031F2C),
    background = Color(0xFF109648),
    onBackground = Color(0xFF072011),
    surface = Color(0xFF109648),
    onSurface = Color(0xFF072011),
    surfaceVariant = Color(0xFF18A954),
    onSurfaceVariant = Color(0xFF0F381E),
    surfaceContainerLowest = Color(0xFF0D823D),
    surfaceContainerLow = Color(0xFF149E4D),
    surfaceContainer = Color(0xFF1CAE58),
    surfaceContainerHigh = Color(0xFF7BE89E),
    surfaceContainerHighest = Color(0xFF9AF5B7),
    outline = Color(0xFF0E4925),
    outlineVariant = Color(0xFF2BC067),
)

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun BrinkScreenTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    val motionScheme = MotionScheme.expressive()
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = NectarTypography,
        shapes = NectarShapes,
        motionScheme = motionScheme,
        content = content,
    )
}
