package com.aquanoodle.sumorater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSumoColors = staticCompositionLocalOf { LightSumoColors }

@Composable
fun SumoRaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkSumoColors else LightSumoColors
    CompositionLocalProvider(LocalSumoColors provides colors, content = content)
}
