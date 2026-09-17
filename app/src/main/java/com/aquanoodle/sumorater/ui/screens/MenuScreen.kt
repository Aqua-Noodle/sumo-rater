package com.aquanoodle.sumorater.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.BashoOption
import com.aquanoodle.sumorater.ui.components.BashoDayPicker
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums

@Composable
fun MenuScreen(
    ratedRikishi: Int,
    bashoLabel: String,
    bashoList: List<BashoOption>,
    day: Int,
    isDark: Boolean,
    onClose: () -> Unit,
    onOpenTable: () -> Unit,
    onPickBasho: (String) -> Unit,
    onPickDay: (Int) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Text("×", fontSize = 22.sp, color = colors.ink)
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, colors.line))
                    .clickable(onClick = onOpenTable)
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("My ratings", style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.ink))
                Text(
                    "$ratedRikishi rikishi →",
                    style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, color = colors.faint, fontFeatureSettings = TabularNums),
                )
            }

            BashoDayPicker(bashoLabel = bashoLabel, bashoList = bashoList, day = day, onPickBasho = onPickBasho, onPickDay = onPickDay)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, colors.line))
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Dark mode", style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = colors.ink))
                ThemeSwitch(isDark = isDark)
            }

            Text(
                "Ratings are stored on this phone only.",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 24.dp),
                style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.faint, lineHeight = 16.sp),
            )
        }
    }
}

@Composable
private fun ThemeSwitch(isDark: Boolean) {
    val colors = LocalSumoColors.current
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .border(BorderStroke(1.dp, colors.ink), androidx.compose.foundation.shape.RoundedCornerShape(50)),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (isDark) 22.dp else 3.dp, top = 3.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(colors.ink),
        )
    }
}
