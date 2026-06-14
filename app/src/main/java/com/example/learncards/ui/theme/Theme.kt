package com.prplegryn.quizcat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.prplegryn.quizcat.R

val QuizcatMono = FontFamily(Font(R.font.jetbrains_mono_regular))
private val QuizcatSans = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.SemiBold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    error = Color(0xFFB42318),
    onError = Color.White,
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF171717),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFF0F1ED),
    onSurfaceVariant = Color(0xFF5A5F68),
    outline = Color(0xFFE2E3DE),
    outlineVariant = Color(0xFFE9EAE5),
    inverseSurface = Color(0xFF202124),
    inverseOnSurface = Color(0xFFF6F6F2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B1B38),
    secondary = Color(0xFF7DD3C7),
    onSecondary = Color(0xFF062C29),
    tertiary = Color(0xFFF6C177),
    onTertiary = Color(0xFF3A2200),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF111214),
    onBackground = Color(0xFFECEDEA),
    surface = Color(0xFF1A1B1E),
    onSurface = Color(0xFFECEDEA),
    surfaceVariant = Color(0xFF25272B),
    onSurfaceVariant = Color(0xFFC6C8C5),
    outline = Color(0xFF36393F),
    outlineVariant = Color(0xFF2E3136),
    inverseSurface = Color(0xFFECEDEA),
    inverseOnSurface = Color(0xFF202124),
)

private val QuizcatTypography = androidx.compose.material3.Typography(
    displaySmall = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = QuizcatSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun QuizcatTheme(
    themeMode: String,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = QuizcatTypography,
        content = content,
    )
}

val ColorScheme.pass: Color
    get() = secondary

val ColorScheme.warning: Color
    get() = tertiary
