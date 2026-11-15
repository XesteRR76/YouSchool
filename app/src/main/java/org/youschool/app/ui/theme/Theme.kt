package org.youschool.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Blue Palette
private val LightBlueScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF)
)
private val DarkBlueScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB)
)

// Purple Palette
private val LightPurpleScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE)
)
private val DarkPurpleScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC)
)

// Green Palette
private val LightGreenScheme = lightColorScheme(
    primary = Color(0xFF2E6B38),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB0F4B2),
    onPrimaryContainer = Color(0xFF002206),
    secondary = Color(0xFF526350),
    background = Color(0xFFFCFDF6),
    surface = Color(0xFFFCFDF6)
)
private val DarkGreenScheme = darkColorScheme(
    primary = Color(0xFF95D797),
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF125222),
    onPrimaryContainer = Color(0xFFB0F4B2),
    secondary = Color(0xFFB9CCB4)
)

// Orange Palette
private val LightOrangeScheme = lightColorScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF725A42),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF)
)
private val DarkOrangeScheme = darkColorScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6B3B00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFDEC2A2)
)

// Red Palette
private val LightRedScheme = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9DEDC),
    onPrimaryContainer = Color(0xFF410E0B),
    secondary = Color(0xFF775656),
    background = Color(0xFFFFFBFB),
    surface = Color(0xFFFFFBFB)
)
private val DarkRedScheme = darkColorScheme(
    primary = Color(0xFFF2B8B5),
    onPrimary = Color(0xFF601410),
    primaryContainer = Color(0xFF8C1D18),
    onPrimaryContainer = Color(0xFFF9DEDC),
    secondary = Color(0xFFE6BDBC)
)

@Composable
fun YouSchoolTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }
    val isAmoled = settings.themeMode == AppThemeMode.AMOLED

    val baseScheme = when (settings.colorPalette) {
        AppColorPalette.MONET -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) DarkPurpleScheme else LightPurpleScheme
            }
        }
        AppColorPalette.BLUE -> if (isDark) DarkBlueScheme else LightBlueScheme
        AppColorPalette.PURPLE -> if (isDark) DarkPurpleScheme else LightPurpleScheme
        AppColorPalette.GREEN -> if (isDark) DarkGreenScheme else LightGreenScheme
        AppColorPalette.ORANGE -> if (isDark) DarkOrangeScheme else LightOrangeScheme
        AppColorPalette.RED -> if (isDark) DarkRedScheme else LightRedScheme
    }

    val finalScheme = if (isAmoled) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color(0xFF0F0F0F),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF080808),
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1C1C1C),
            surfaceVariant = Color(0xFF121212)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}
