package com.aquanoodle.sumorater.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.BashoOption
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums

/** "Basho" label + select, and "Day" label + 5-column grid — shared by Menu and Start. */
@Composable
fun BashoDayPicker(
    bashoLabel: String,
    bashoList: List<BashoOption>,
    day: Int,
    onPickBasho: (String) -> Unit,
    onPickDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, LocalSumoColors.current.line))
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
        ) {
            SectionLabel("Basho")
            Spacer(modifier = Modifier.height(10.dp))
            BashoSelect(bashoLabel = bashoLabel, bashoList = bashoList, onPick = onPickBasho)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 18.dp)) {
            SectionLabel("Day")
            Spacer(modifier = Modifier.height(10.dp))
            DayGrid(selectedDay = day, onPick = onPickDay)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    val colors = LocalSumoColors.current
    Text(
        text.uppercase(),
        style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.14f.em, color = colors.faint),
    )
}

@Composable
fun BashoSelect(bashoLabel: String, bashoList: List<BashoOption>, onPick: (String) -> Unit) {
    val colors = LocalSumoColors.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, colors.ink))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(bashoLabel, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = colors.ink))
            Text("▾", style = TextStyle(fontFamily = Archivo, fontSize = 16.sp, color = colors.ink))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            bashoList.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onPick(option.id)
                    },
                )
            }
        }
    }
}

@Composable
fun DayGrid(selectedDay: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..15).chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { d ->
                    DayButton(
                        day = d,
                        selected = d == selectedDay,
                        onClick = { onPick(d) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayButton(day: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSumoColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .border(BorderStroke(1.dp, colors.line))
            .background(if (selected) colors.ink else colors.bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            style = TextStyle(
                fontFamily = Archivo,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (selected) colors.bg else colors.ink,
                fontFeatureSettings = TabularNums,
            ),
        )
    }
}
