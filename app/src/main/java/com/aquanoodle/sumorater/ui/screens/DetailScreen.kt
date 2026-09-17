package com.aquanoodle.sumorater.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.ChartData
import com.aquanoodle.sumorater.data.ChartMode
import com.aquanoodle.sumorater.data.RikishiAggregate
import com.aquanoodle.sumorater.data.RikishiEntry
import com.aquanoodle.sumorater.data.bashoLabel
import com.aquanoodle.sumorater.ui.components.BoutLineChart
import com.aquanoodle.sumorater.ui.components.RikishiPhoto
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.LossRowBg
import com.aquanoodle.sumorater.ui.theme.LossRowFg
import com.aquanoodle.sumorater.ui.theme.TabularNums
import com.aquanoodle.sumorater.ui.theme.WinRowBg
import com.aquanoodle.sumorater.ui.theme.WinRowFg

@Composable
fun DetailScreen(
    rikishi: RikishiAggregate,
    photoPath: String?,
    chart: ChartData,
    mode: ChartMode,
    onBack: () -> Unit,
    onModeChange: (ChartMode) -> Unit,
    onPhotoTap: () -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Text("←", fontSize = 20.sp, color = colors.ink)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RikishiPhoto(
                        photoPath = photoPath,
                        onClick = onPhotoTap,
                        modifier = Modifier.size(width = 72.dp, height = 92.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            rikishi.name,
                            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.02f).em, color = colors.ink),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(rikishi.rank, style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, letterSpacing = 0.04f.em, color = colors.mute))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            rikishi.avg,
                            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.03f).em, color = colors.ink, fontFeatureSettings = TabularNums),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "avg · ${rikishi.count}".uppercase(),
                            style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Rating over time".uppercase(),
                        style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
                    )
                    Row(modifier = Modifier.border(androidx.compose.foundation.BorderStroke(1.dp, colors.ink))) {
                        ModeButton("Days", selected = mode == ChartMode.DAY, onClick = { onModeChange(ChartMode.DAY) })
                        ModeButton("Basho", selected = mode == ChartMode.BASHO, onClick = { onModeChange(ChartMode.BASHO) })
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                    BoutLineChart(chart = chart)
                }
            }

            items(rikishi.list.reversed()) { entry ->
                RatingRow(entry)
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    Box(
        modifier = Modifier
            .background(if (selected) colors.ink else colors.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Archivo,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = if (selected) colors.bg else colors.ink,
            ),
        )
    }
}

@Composable
private fun RatingRow(entry: RikishiEntry) {
    val colors = LocalSumoColors.current
    val bg = if (entry.won) WinRowBg else LossRowBg
    val fg = if (entry.won) WinRowFg else LossRowFg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .drawBehind {
                drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("vs ${entry.opp}", style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${bashoLabel(entry.bashoId)} · Day ${entry.day}",
                style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.04f.em, color = fg.copy(alpha = 0.7f)),
            )
        }
        Text(
            (if (entry.won) "win" else "loss").uppercase(),
            style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.14f.em, color = fg.copy(alpha = 0.7f)),
        )
        Text(
            entry.rating.toString(),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = fg, fontFeatureSettings = TabularNums),
        )
    }
}
