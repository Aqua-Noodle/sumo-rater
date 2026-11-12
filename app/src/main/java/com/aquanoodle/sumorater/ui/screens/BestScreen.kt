package com.aquanoodle.sumorater.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.aquanoodle.sumorater.data.BestScope
import com.aquanoodle.sumorater.data.Rating
import com.aquanoodle.sumorater.data.bashoLabel
import com.aquanoodle.sumorater.data.kimariteLabel
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums

/** Bouts ranked by combined score (winner + loser) for the open day, basho, or all time. */
@Composable
fun BestScreen(
    bouts: List<Pair<String, Rating>>,
    scope: BestScope,
    bashoLabel: String,
    day: Int,
    onScope: (BestScope) -> Unit,
    onBack: () -> Unit,
    onOpenBout: (key: String, rating: Rating) -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Text("←", fontSize = 20.sp, color = colors.ink)
            }
            Text(
                "Best bouts".uppercase(),
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, letterSpacing = 0.14f.em, color = colors.mute),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.border(1.dp, colors.ink)) {
                BestScope.entries.forEach { s ->
                    val on = s == scope
                    Box(
                        modifier = Modifier
                            .background(if (on) colors.ink else colors.bg)
                            .clickable { onScope(s) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(s.label, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = if (on) colors.bg else colors.ink))
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                when (scope) {
                    BestScope.DAY -> "$bashoLabel · Day $day"
                    BestScope.BASHO -> bashoLabel
                    BestScope.ALL -> "${bouts.size} bouts"
                },
                style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.faint),
            )
        }

        if (bouts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No rated bouts here yet.",
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.faint),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                itemsIndexed(bouts.take(100), key = { _, b -> b.first }) { i, (key, r) ->
                    BestRow(rank = i + 1, r = r, onClick = { onOpenBout(key, r) })
                }
            }
        }
    }
}

@Composable
private fun BestRow(rank: Int, r: Rating, onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "$rank",
            modifier = Modifier.width(28.dp),
            style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, color = colors.faint, fontFeatureSettings = TabularNums),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${r.winnerName} def. ${r.loserName}",
                style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.ink),
            )
            val details = listOfNotNull("${bashoLabel(r.bashoId)} · Day ${r.day}", r.div, kimariteLabel(r.kimarite)) + r.tags
            Text(
                details.joinToString(" · "),
                style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.mute),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${r.total}",
                style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.ink, fontFeatureSettings = TabularNums),
            )
            Text(
                "${r.w ?: "–"} + ${r.l ?: "–"}",
                style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.faint, fontFeatureSettings = TabularNums),
            )
        }
    }
}
