package com.aquanoodle.sumorater.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.BashoOption
import com.aquanoodle.sumorater.ui.components.BashoDayPicker
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors

/**
 * The first screen on every cold launch: pick a basho/day before anything is
 * fetched or shown, so opening the app never spoils a bout you haven't
 * watched yet. Rating only starts once you confirm.
 */
@Composable
fun StartScreen(
    bashoLabel: String,
    bashoList: List<BashoOption>,
    day: Int,
    ratedRikishi: Int,
    onPickBasho: (String) -> Unit,
    onPickDay: (Int) -> Unit,
    onStart: () -> Unit,
    onOpenTable: () -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Text(
            "Sumo Rater",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.02f).em, color = colors.ink),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pick a basho and day. Nothing loads until you choose, so you can rate at your own pace without seeing who won first.",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.mute, lineHeight = 20.sp),
        )

        Spacer(modifier = Modifier.height(28.dp))
        BashoDayPicker(bashoLabel = bashoLabel, bashoList = bashoList, day = day, onPickBasho = onPickBasho, onPickDay = onPickDay)

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
        ) {
            Text("Rate bouts →", style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 14.sp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onOpenTable, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "My ratings ($ratedRikishi rikishi) →",
                style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, color = colors.faint),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
