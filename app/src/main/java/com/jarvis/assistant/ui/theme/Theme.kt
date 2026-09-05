package com.jarvis.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisBackground,
    secondary = JarvisAmber,
    background = JarvisBackground,
    surface = JarvisSurface,
    onBackground = JarvisText,
    onSurface = JarvisText,
    error = JarvisRed
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    // Jarvis is always dark-themed by design (the futuristic HUD look depends on it),
    // matching the "Theme" setting exposed in Settings for future light-mode support.
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = JarvisTypography,
        content = content
    )
}
