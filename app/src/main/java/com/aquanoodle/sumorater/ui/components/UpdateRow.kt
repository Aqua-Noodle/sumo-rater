package com.aquanoodle.sumorater.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.AppUpdater
import com.aquanoodle.sumorater.data.UpdateState
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import kotlin.math.roundToInt

/** "Check for updates" row: checks GitHub, then downloads and hands the APK to Android's installer. */
@Composable
fun UpdateRow(updater: AppUpdater, modifier: Modifier = Modifier) {
    val colors = LocalSumoColors.current
    val (title, detail, action) = when (val s = updater.state) {
        UpdateState.Idle -> Triple("Check for updates", "Installed: build ${updater.currentBuild}", "↻")
        UpdateState.Checking -> Triple("Checking for updates…", "Installed: build ${updater.currentBuild}", "")
        UpdateState.UpToDate -> Triple("You're up to date", "Installed: build ${updater.currentBuild}", "↻")
        is UpdateState.Available -> Triple("Update available: build ${s.build}", "Installed: build ${updater.currentBuild}", "Install →")
        is UpdateState.Downloading -> Triple("Downloading build ${s.build}…", "${(s.progress * 100).roundToInt()}%", "")
        is UpdateState.ReadyToInstall -> Triple("Build ${s.build} downloaded", "Tap if the installer didn't open", "Install →")
        is UpdateState.NeedsInstallPermission ->
            Triple("Allow installs from Sumo Rater", "Turn on the switch, come back, tap here", "Install →")
        is UpdateState.Failed -> Triple("Update check failed", s.message, "Retry")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, colors.line))
            .clickable(onClick = updater::onTap)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = colors.ink))
            Text(detail, style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.faint))
        }
        Text(action, style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, color = colors.ink))
    }
}
