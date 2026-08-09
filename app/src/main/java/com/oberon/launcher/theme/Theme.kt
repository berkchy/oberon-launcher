package com.oberon.launcher.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

@Composable
fun OberonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentOption: String = "custom",
    accentColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        accentOption == "dynamic" && Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> {
            val seed = if (accentOption == "dynamic") Color(0xFF6750A4) else accentColor
            val base = if (darkTheme) darkColorScheme() else lightColorScheme()
            base.copy(
                primary = seed,
                onPrimary = if (seed.luminance() > 0.5f) Color.White else Color.Black,
                primaryContainer = seed.copy(alpha = 0.2f),
                onPrimaryContainer = seed,
                secondary = seed.copy(alpha = 0.8f),
                tertiary = seed.copy(alpha = 0.6f),
                surfaceContainerLow = if (darkTheme) Color(0xFF1C1B1F) else Color(0xFFF7F2FA)
            )
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}