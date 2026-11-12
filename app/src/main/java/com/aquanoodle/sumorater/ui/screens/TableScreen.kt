package com.aquanoodle.sumorater.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.border
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aquanoodle.sumorater.data.BashoOption
import com.aquanoodle.sumorater.data.TableSort
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
import com.aquanoodle.sumorater.data.RikishiAggregate
import com.aquanoodle.sumorater.ui.components.RikishiPhoto
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums

@Composable
fun TableScreen(
    rows: List<RikishiAggregate>,
    photos: Map<Int, String>,
    onBack: () -> Unit,
    onOpenDetail: (Int) -> Unit,
    onExportCsv: () -> Unit,
    hasAnyRatings: Boolean,
    bashoOptions: List<BashoOption>,
    selectedBasho: String?,
    selectedDiv: String?,
    sort: TableSort,
    onFilter: (bashoId: String?, div: String?) -> Unit,
    onSort: (TableSort) -> Unit,
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
                "My ratings".uppercase(),
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, letterSpacing = 0.14f.em, color = colors.mute),
            )
            if (hasAnyRatings) ExportCsvButton(onClick = onExportCsv)
        }

        if (hasAnyRatings) {
            FilterBar(
                bashoOptions = bashoOptions,
                selectedBasho = selectedBasho,
                selectedDiv = selectedDiv,
                sort = sort,
                onFilter = onFilter,
                onSort = onSort,
            )
        }

        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (hasAnyRatings) "No rated bouts match these filters." else "No ratings yet. Rate a bout and the rikishi appear here.",
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.faint, lineHeight = 21.sp),
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp)) {
                Text(
                    "Rikishi".uppercase(),
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
                )
                Text(
                    "Avg".uppercase(),
                    modifier = Modifier.width(56.dp),
                    textAlign = TextAlign.End,
                    style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
                )
                Text(
                    "N".uppercase(),
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows, key = { it.id }) { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                            }
                            .clickable { onOpenDetail(r.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        RikishiPhoto(
                            photoPath = photos[r.id],
                            onClick = { onOpenDetail(r.id) },
                            modifier = Modifier.size(width = 36.dp, height = 46.dp),
                            cornerRadius = 4.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(r.name, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.ink))
                            Text(r.rank, style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.04f.em, color = colors.mute))
                        }
                        Text(
                            r.avg,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.End,
                            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.ink, fontFeatureSettings = TabularNums),
                        )
                        Text(
                            r.count.toString(),
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End,
                            style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.faint, fontFeatureSettings = TabularNums),
                        )
                    }
                }
            }
        }
    }
}

/** Basho + division filters and sort order; averages are recomputed for the filtered bouts. */
@Composable
private fun FilterBar(
    bashoOptions: List<BashoOption>,
    selectedBasho: String?,
    selectedDiv: String?,
    sort: TableSort,
    onFilter: (bashoId: String?, div: String?) -> Unit,
    onSort: (TableSort) -> Unit,
) {
    val colors = LocalSumoColors.current
    var bashoMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.ink)
                    .clickable { bashoMenu = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    bashoOptions.firstOrNull { it.id == selectedBasho }?.label ?: "All basho",
                    style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.ink),
                )
                Text("▾", style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.ink))
            }
            DropdownMenu(expanded = bashoMenu, onDismissRequest = { bashoMenu = false }) {
                DropdownMenuItem(text = { Text("All basho") }, onClick = { bashoMenu = false; onFilter(null, selectedDiv) })
                bashoOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label) }, onClick = { bashoMenu = false; onFilter(option.id, selectedDiv) })
                }
            }
        }
        Segmented(
            options = listOf<Pair<String?, String>>(null to "All", "Makuuchi" to "Makuuchi", "Juryo" to "Juryo"),
            selected = selectedDiv,
            onSelect = { onFilter(selectedBasho, it) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SORT",
                modifier = Modifier.padding(end = 10.dp),
                style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
            )
            Segmented(
                options = TableSort.entries.map { it to it.label },
                selected = sort,
                onSelect = onSort,
            )
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val colors = LocalSumoColors.current
    Row(modifier = Modifier.border(1.dp, colors.ink)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .background(if (on) colors.ink else colors.bg)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(label, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = if (on) colors.bg else colors.ink))
            }
        }
    }
}
