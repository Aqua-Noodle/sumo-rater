package com.aquanoodle.sumorater.ui.screens

import com.aquanoodle.sumorater.data.dayLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aquanoodle.sumorater.data.kimariteLabel
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
    onEditEntry: (RikishiEntry) -> Unit,
    onExportCsv: () -> Unit,
    onOpenRikishi: (Int) -> Unit,
    /** Bouts matching the "My ratings" search, or null when no filter is set. */
    matchingKeys: Set<String>? = null,
) {
    val colors = LocalSumoColors.current
    // Coming from a filtered overview, start with just the bouts that matched.
    var onlyMatching by remember(rikishi.id, matchingKeys) { mutableStateOf(matchingKeys != null) }
    val matching = matchingKeys?.let { keys -> rikishi.list.filter { it.key in keys } }
    val base = if (onlyMatching && matching != null) matching else rikishi.list
    // Head-to-head: when an opponent is picked, the bout list shows only bouts against them.
    var opponent by remember(rikishi.id) { mutableStateOf<Int?>(null) }
    // Point tapped in the line chart (index into the chart's points).
    var picked by remember(rikishi.id, mode) { mutableStateOf<Int?>(null) }
    val shown = opponent?.let { id -> base.filter { it.oppId == id } } ?: base
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
            Spacer(modifier = Modifier.weight(1f))
            ExportCsvButton(onClick = onExportCsv)
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
                    BoutLineChart(chart = chart, selected = picked, onSelect = { picked = if (picked == it) null else it })
                }
            }

            item {
                ChartPick(
                    list = rikishi.list,
                    mode = mode,
                    index = picked,
                    onEdit = onEditEntry,
                    onOpenRikishi = onOpenRikishi,
                )
            }

            item { RatingDistribution(rikishi.list) }

            item {
                HeadToHead(
                    entries = rikishi.list,
                    selected = opponent,
                    onSelect = { opponent = if (opponent == it) null else it },
                )
            }

            if (matching != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (onlyMatching) "${matching.size} of ${rikishi.list.size} bouts match your filters"
                            else "All ${rikishi.list.size} bouts",
                            modifier = Modifier.weight(1f),
                            style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.mute, fontFeatureSettings = TabularNums),
                        )
                        TextButton(onClick = { onlyMatching = !onlyMatching }) {
                            Text(
                                if (onlyMatching) "Show all" else "Only matches",
                                style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.ink),
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Tap a bout to change its rating, tags or note.",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.faint),
                )
            }

            items(shown.reversed(), key = { it.key }) { entry ->
                RatingRow(entry, onClick = { onEditEntry(entry) }, onOpponentTap = { onOpenRikishi(entry.oppId) })
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
fun ExportCsvButton(onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    Text(
        "EXPORT CSV",
        modifier = Modifier
            .sizeIn(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center)
            .padding(horizontal = 12.dp),
        style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.14f.em, color = colors.ink),
    )
}

@Composable
private fun RatingRow(entry: RikishiEntry, onClick: () -> Unit, onOpponentTap: () -> Unit) {
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
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "vs ${entry.opp}",
                modifier = Modifier.clickable(onClick = onOpponentTap),
                style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${bashoLabel(entry.bashoId)} · ${dayLabel(entry.day)}",
                style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.04f.em, color = fg.copy(alpha = 0.7f)),
            )
            val extra = listOfNotNull(kimariteLabel(entry.kimarite)) + entry.tags
            if (extra.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    extra.joinToString(" · "),
                    style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = fg.copy(alpha = 0.7f)),
                )
            }
            if (!entry.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    entry.note,
                    style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, lineHeight = 16.sp, color = fg.copy(alpha = 0.85f)),
                )
            }
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

/** The bout (or basho average) behind the tapped chart point; a hint until one is tapped. */
@Composable
private fun ChartPick(
    list: List<RikishiEntry>,
    mode: ChartMode,
    index: Int?,
    onEdit: (RikishiEntry) -> Unit,
    onOpenRikishi: (Int) -> Unit,
) {
    val colors = LocalSumoColors.current
    val hintStyle = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.faint)
    if (index == null) {
        Text("Tap a point to see that bout.", modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp), style = hintStyle)
        return
    }
    if (mode == ChartMode.DAY) {
        val entry = list.getOrNull(index) ?: return
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).border(1.dp, colors.ink)) {
            RatingRow(entry, onClick = { onEdit(entry) }, onOpponentTap = { onOpenRikishi(entry.oppId) })
        }
    } else {
        // Same grouping and order as buildChart's basho mode.
        val (bashoId, bouts) = list.groupBy { it.bashoId }.toSortedMap().entries.toList().getOrNull(index) ?: return
        val wins = bouts.count { it.won }
        val avg = String.format(java.util.Locale.US, "%.1f", bouts.map { it.rating }.average())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .border(1.dp, colors.ink)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bashoLabel(bashoId), style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.ink))
                Text(
                    "${bouts.size} rated bout${if (bouts.size == 1) "" else "s"} · $wins–${bouts.size - wins}",
                    style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.mute, fontFeatureSettings = TabularNums),
                )
            }
            Text(avg, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.ink, fontFeatureSettings = TabularNums))
        }
    }
}

/** How often each score was given: ratings 0–10 on the vertical axis (10 on top), bar length = bouts. */
@Composable
private fun RatingDistribution(list: List<RikishiEntry>) {
    val colors = LocalSumoColors.current
    if (list.isEmpty()) return
    val counts = IntArray(11)
    list.forEach { counts[it.rating.coerceIn(0, 10)]++ }
    val max = counts.max().coerceAtLeast(1)
    val small = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.faint, fontFeatureSettings = TabularNums)
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
        Text(
            "RATING DISTRIBUTION",
            style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
        )
        Spacer(modifier = Modifier.height(8.dp))
        (10 downTo 0).forEach { v ->
            val n = counts[v]
            Row(modifier = Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$v", modifier = Modifier.width(22.dp), textAlign = TextAlign.End, style = small)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp)
                        // y axis
                        .drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
                        .padding(vertical = 3.dp),
                ) {
                    if (n > 0) {
                        Box(modifier = Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight().background(colors.ink))
                    }
                }
                Text(if (n > 0) "$n" else "", modifier = Modifier.width(30.dp), textAlign = TextAlign.End, style = small)
            }
        }
    }
}

/** Opponents faced, most bouts first; tap one to filter the list to that matchup. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeadToHead(entries: List<RikishiEntry>, selected: Int?, onSelect: (Int) -> Unit) {
    val colors = LocalSumoColors.current
    val byOpp = entries.groupBy { it.oppId }.values
        .sortedWith(compareByDescending<List<RikishiEntry>> { it.size }.thenBy { it.last().opp })
    if (byOpp.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
        Text(
            "HEAD-TO-HEAD",
            style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            byOpp.forEach { bouts ->
                val id = bouts.first().oppId
                val on = id == selected
                val wins = bouts.count { it.won }
                Box(
                    modifier = Modifier
                        .border(1.dp, if (on) colors.ink else colors.line)
                        .background(if (on) colors.ink else colors.bg)
                        .clickable { onSelect(id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "${bouts.last().opp} ${wins}–${bouts.size - wins}",
                        style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = if (on) colors.bg else colors.ink, fontFeatureSettings = TabularNums),
                    )
                }
            }
        }
        selected?.let { id ->
            val vs = entries.filter { it.oppId == id }
            val avg = vs.map { it.rating }.average()
            Text(
                "Avg ${String.format(java.util.Locale.US, "%.1f", avg)} in ${vs.size} bout${if (vs.size == 1) "" else "s"} vs ${vs.last().opp}",
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.mute),
            )
        }
    }
}
