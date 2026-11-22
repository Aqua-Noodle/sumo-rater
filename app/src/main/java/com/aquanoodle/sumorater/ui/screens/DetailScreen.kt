package com.aquanoodle.sumorater.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.aquanoodle.sumorater.data.buildChart
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aquanoodle.sumorater.data.kimariteLabel
import com.aquanoodle.sumorater.data.tagCounts
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
    mode: ChartMode,
    onBack: () -> Unit,
    onModeChange: (ChartMode) -> Unit,
    onPhotoTap: () -> Unit,
    onEditEntry: (RikishiEntry) -> Unit,
    onExportCsv: () -> Unit,
    onOpenRikishi: (Int) -> Unit,
    /** Bouts matching the "My ratings" search, or null when no filter is set. */
    matchingKeys: Set<String>? = null,
    showOpponents: Boolean = false,
    onToggleOpponents: () -> Unit = {},
    /** Place in the My ratings list ("3 / 42"), and its neighbours; null when not in the list. */
    position: String? = null,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val colors = LocalSumoColors.current
    // Coming from a filtered overview, start with just the bouts that matched.
    var onlyMatching by remember(rikishi.id, matchingKeys) { mutableStateOf(matchingKeys != null) }
    val matching = matchingKeys?.let { keys -> rikishi.list.filter { it.key in keys } }
    val base = if (onlyMatching && matching != null) matching else rikishi.list
    val shown = base
    // Days view can be narrowed to one basho (null = all).
    var chartBasho by remember(rikishi.id) { mutableStateOf<String?>(null) }
    val bashoIds = remember(rikishi.list) { rikishi.list.map { it.bashoId }.distinct().sortedDescending() }
    val chartList = if (mode == ChartMode.DAY && chartBasho != null) rikishi.list.filter { it.bashoId == chartBasho } else rikishi.list
    val chart = remember(chartList, mode) { buildChart(chartList, mode) }
    // Point tapped in the line chart (index into the chart's points).
    var picked by remember(rikishi.id, mode, chartBasho) { mutableStateOf<Int?>(null) }
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
            if (position != null) {
                val arrow = TextStyle(fontFamily = Archivo, fontSize = 18.sp, color = colors.ink)
                IconButton(onClick = { onPrev?.invoke() }, enabled = onPrev != null, modifier = Modifier.size(40.dp)) {
                    Text("‹", style = arrow.copy(color = if (onPrev != null) colors.ink else colors.line))
                }
                Text(
                    position,
                    style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.08f.em, color = colors.faint, fontFeatureSettings = TabularNums),
                )
                IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null, modifier = Modifier.size(40.dp)) {
                    Text("›", style = arrow.copy(color = if (onNext != null) colors.ink else colors.line))
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            ExportCsvButton(onClick = onExportCsv)
        }

        // Swipe left/right anywhere on the page for the next/previous wrestler in the list.
        val density = LocalDensity.current
        val threshold = with(density) { 80.dp.toPx() }
        var dragX by remember { mutableFloatStateOf(0f) }
        val latestPrev by rememberUpdatedState(onPrev)
        val latestNext by rememberUpdatedState(onNext)
        val swipe = if (onPrev == null && onNext == null) Modifier else Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val dx = dragX
                    dragX = 0f
                    when {
                        dx <= -threshold -> latestNext?.invoke()
                        dx >= threshold -> latestPrev?.invoke()
                    }
                },
                onDragCancel = { dragX = 0f },
            ) { change, amount ->
                change.consume()
                dragX += amount
            }
        }

        // A different wrestler starts at the top of their page.
        val listState = rememberLazyListState()
        LaunchedEffect(rikishi.id) { listState.scrollToItem(0) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .then(swipe)
                .graphicsLayer {
                    // Follow the finger a little (damped), so the swipe feels connected.
                    translationX = dragX * 0.35f
                    alpha = 1f - (kotlin.math.abs(dragX) / (threshold * 4)).coerceIn(0f, 0.4f)
                },
        ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (mode == ChartMode.DAY && bashoIds.size > 1) {
                        ChartBashoPicker(bashoIds = bashoIds, selected = chartBasho, onPick = { chartBasho = it })
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.border(androidx.compose.foundation.BorderStroke(1.dp, if (showOpponents) colors.ink else colors.line))) {
                        ModeButton("Opponents", selected = showOpponents, onClick = onToggleOpponents)
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                    BoutLineChart(
                        chart = chart,
                        selected = picked,
                        onSelect = { picked = if (picked == it) null else it },
                        showOpponents = showOpponents,
                    )
                }
            }

            if (showOpponents) {
                item { ChartLegend(name = rikishi.name) }
            }

            item {
                ChartPick(
                    list = chartList,
                    mode = mode,
                    index = picked,
                    onEdit = onEditEntry,
                    onOpenRikishi = onOpenRikishi,
                )
            }

            item { RatingDistribution(rikishi.list) }

            item { TagChart(rikishi.list) }

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

/**
 * Tags credited to this wrestler: shared ones (tachiai, long bout, close call), Upset when
 * they won, and Henka/Injury when assigned to them. Matta, Monoii, Torinaoshi aren't anyone's.
 */
@Composable
private fun TagChart(list: List<RikishiEntry>) {
    val colors = LocalSumoColors.current
    val counts = tagCounts(list)
    if (counts.isEmpty()) return
    val max = counts.maxOf { it.second }.coerceAtLeast(1)
    val small = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.faint, fontFeatureSettings = TabularNums)
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
        Text(
            "TAGS",
            style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
        )
        Spacer(modifier = Modifier.height(8.dp))
        counts.forEach { (tag, n) ->
            Row(modifier = Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tag,
                    modifier = Modifier.width(104.dp),
                    maxLines = 1,
                    style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.ink),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp)
                        .drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
                        .padding(vertical = 4.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(n.toFloat() / max).fillMaxHeight().background(colors.ink))
                }
                Text("$n", modifier = Modifier.width(30.dp), textAlign = TextAlign.End, style = small)
            }
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

/** Narrows the Days chart to one basho. */
@Composable
private fun ChartBashoPicker(bashoIds: List<String>, selected: String?, onPick: (String?) -> Unit) {
    val colors = LocalSumoColors.current
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .border(1.dp, colors.ink)
                .clickable { open = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                (selected?.let { bashoLabel(it) } ?: "All basho") + "  ▾",
                style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = colors.ink),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("All basho") }, onClick = { open = false; onPick(null) })
            bashoIds.forEach { id ->
                DropdownMenuItem(text = { Text(bashoLabel(id)) }, onClick = { open = false; onPick(id) })
            }
        }
    }
}

/** Which line is whose once the opponents' line is on. */
@Composable
private fun ChartLegend(name: String) {
    val colors = LocalSumoColors.current
    val style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, color = colors.mute)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(width = 18.dp, height = 8.dp)) {
            drawLine(colors.ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
        }
        Text(name, modifier = Modifier.padding(start = 6.dp, end = 16.dp), style = style)
        Canvas(modifier = Modifier.size(width = 18.dp, height = 8.dp)) {
            drawLine(
                colors.faint, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
        }
        Text("Opponents", modifier = Modifier.padding(start = 6.dp), style = style)
    }
}
