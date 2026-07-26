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
    primary = NovaPrimaryDark,
    secondary = NovaSecondaryDark,
    tertiary = NovaTertiaryDark,
    background = NovaBackgroundDark,
    surface = NovaSurfaceDark,
    error = NovaError,
    onPrimary = NovaSurfaceDark,
    onSecondary = NovaSurfaceDark,
    onBackground = NovaSurface,
    onSurface = NovaSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NovaPrimary,
    secondary = NovaSecondary,
    tertiary = NovaTertiary,
    background = NovaBackground,
    surface = NovaSurface,
    error = NovaError,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1D21),
    onSurface = Color(0xFF1C1D21)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the high-quality fintech brand identity
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
