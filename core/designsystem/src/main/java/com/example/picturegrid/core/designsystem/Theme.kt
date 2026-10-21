package com.example.picturegrid.core.designsystem

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EditorialTerracottaDark,
    secondary = EditorialOliveDark,
    tertiary = EditorialSandDark,
    background = Color(0xFF211A17),
    surface = Color(0xFF211A17),
    surfaceVariant = Color(0xFF51443E),
)

private val LightColorScheme = lightColorScheme(
    primary = EditorialTerracottaLight,
    secondary = EditorialOliveLight,
    tertiary = EditorialSandLight,
    background = Color(0xFFFBF8F3),
    surface = Color(0xFFFFFCF8),
    surfaceVariant = Color(0xFFEDE4DA),
)

@Composable
fun PictureGridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
