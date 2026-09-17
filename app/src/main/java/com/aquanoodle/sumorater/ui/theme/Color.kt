package com.aquanoodle.sumorater.ui.theme

import androidx.compose.ui.graphics.Color

/** Design tokens, ported 1:1 from the design handoff. */
data class SumoColors(
    val bg: Color,
    val ink: Color,
    val mute: Color,
    val faint: Color,
    val line: Color,
    val silhouetteBg: Color,
    val silhouetteFg: Color,
    val isDark: Boolean,
)

val LightSumoColors = SumoColors(
    bg = Color(0xFFF4F1EB),
    ink = Color(0xFF141311),
    mute = Color(0xFF5A564F),
    faint = Color(0xFF9A958B),
    line = Color(0xFFE4E0D7),
    silhouetteBg = Color(0xFFE6E2D9),
    silhouetteFg = Color(0xFFCFCAC0),
    isDark = false,
)

val DarkSumoColors = SumoColors(
    bg = Color(0xFF161513),
    ink = Color(0xFFF1EDE6),
    mute = Color(0xFFA8A39A),
    faint = Color(0xFF6E6A63),
    line = Color(0xFF2A2926),
    silhouetteBg = Color(0xFF26251F),
    silhouetteFg = Color(0xFF3A3832),
    isDark = true,
)

// Fixed regardless of theme: win rows render white-on-black, loss rows black-on-white.
val WinRowBg = Color(0xFFFFFFFF)
val WinRowFg = Color(0xFF000000)
val LossRowBg = Color(0xFF000000)
val LossRowFg = Color(0xFFFFFFFF)
