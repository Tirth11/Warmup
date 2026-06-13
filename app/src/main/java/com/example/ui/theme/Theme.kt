package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = HighDensityLime,
    secondary = HighDensityPeach,
    tertiary = HighDensityCoral,
    background = Color(0xFF1F1B1A),
    surface = HighDensityCoffee,
    onPrimary = HighDensityOnLime,
    onSecondary = HighDensityCoffee,
    onBackground = HighDensityBg,
    onSurface = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityCoffee,
    secondary = HighDensityPeach,
    tertiary = HighDensityCoral,
    background = HighDensityBg,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = HighDensityCoffee,
    onBackground = HighDensityText,
    onSurface = HighDensityCoffee
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default so that our pristine design colors shine on all targets
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
