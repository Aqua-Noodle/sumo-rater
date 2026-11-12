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
import com.aquanoodle.sumorater.data.BoutFilter
import com.aquanoodle.sumorater.data.QUICK_TAGS
import com.aquanoodle.sumorater.data.kimariteLabel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
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
    matchingBouts: Int,
    bashoOptions: List<BashoOption>,
    kimariteOptions: List<String>,
    filter: BoutFilter,
    sort: TableSort,
    onFilter: (BoutFilter) -> Unit,
    onSort: (TableSort) -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
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

        if (!hasAnyRatings) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No ratings yet. Rate a bout and the rikishi appear here.",
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.faint, lineHeight = 21.sp),
                )
            }
        } else {
            // Filters live inside the list so they scroll away and leave room for results.
            LazyColumn(modifier = Modifier.weight(1f)) {
                item(key = "filters") {
                    FilterBar(
                        bashoOptions = bashoOptions,
                        kimariteOptions = kimariteOptions,
                        filter = filter,
                        sort = sort,
                        onFilter = onFilter,
                        onSort = onSort,
                    )
                }
                if (filter.isActive) {
                    item(key = "summary") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "$matchingBouts ${if (matchingBouts == 1) "bout matches" else "bouts match"}",
                                modifier = Modifier.weight(1f),
                                style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.mute, fontFeatureSettings = TabularNums),
                            )
                            TextButton(onClick = { onFilter(BoutFilter()) }) {
                                Text("Clear filters", style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.ink))
                            }
                        }
                    }
                }

                if (rows.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "No rated bouts match these filters.",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.faint, lineHeight = 21.sp),
                        )
                    }
                } else {
                    item(key = "header") {
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
                    }
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
}

/** Keyword search, basho/kimarite/division/tag filters and sort order; averages cover only matching bouts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBar(
    bashoOptions: List<BashoOption>,
    kimariteOptions: List<String>,
    filter: BoutFilter,
    sort: TableSort,
    onFilter: (BoutFilter) -> Unit,
    onSort: (TableSort) -> Unit,
) {
    val colors = LocalSumoColors.current
    val focus = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicTextField(
            value = filter.query,
            onValueChange = { onFilter(filter.copy(query = it)) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().border(1.dp, colors.ink),
            decorationBox = { inner ->
                Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                        if (filter.query.isEmpty()) {
                            Text(
                                "Search notes, names, kimarite…",
                                style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.faint),
                            )
                        }
                        inner()
                    }
                    if (filter.query.isNotEmpty()) {
                        Text(
                            "×",
                            modifier = Modifier
                                .clickable { onFilter(filter.copy(query = "")) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            style = TextStyle(fontFamily = Archivo, fontSize = 18.sp, color = colors.mute),
                        )
                    }
                }
            },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown(
                label = bashoOptions.firstOrNull { it.id == filter.bashoId }?.label ?: "All basho",
                options = listOf<Pair<String?, String>>(null to "All basho") + bashoOptions.map { it.id to it.label },
                onSelect = { onFilter(filter.copy(bashoId = it)) },
                modifier = Modifier.weight(1f),
            )
            Dropdown(
                label = kimariteLabel(filter.kimarite) ?: "Any kimarite",
                options = listOf<Pair<String?, String>>(null to "Any kimarite") + kimariteOptions.map { it to (kimariteLabel(it) ?: it) },
                onSelect = { onFilter(filter.copy(kimarite = it)) },
                modifier = Modifier.weight(1f),
            )
        }

        Segmented(
            options = listOf<Pair<String?, String>>(null to "All", "Makuuchi" to "Makuuchi", "Juryo" to "Juryo"),
            selected = filter.div,
            onSelect = { onFilter(filter.copy(div = it)) },
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QUICK_TAGS.forEach { tag ->
                val on = tag in filter.tags
                Box(
                    modifier = Modifier
                        .border(1.dp, if (on) colors.ink else colors.line)
                        .background(if (on) colors.ink else colors.bg)
                        .clickable { onFilter(filter.copy(tags = if (on) filter.tags - tag else filter.tags + tag)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(tag, style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = if (on) colors.bg else colors.ink))
                }
            }
        }

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
private fun <T> Dropdown(label: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSumoColors.current
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.ink)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.ink),
            )
            Text("▾", style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.ink))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onSelect(value) })
            }
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
