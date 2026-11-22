package com.aquanoodle.sumorater.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.ChartData
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors

/**
 * 340x150 logical-unit line chart, matching the detail screen's SVG exactly.
 * Tapping near a point reports its index through [onSelect]; [selected] is drawn highlighted.
 */
@Composable
fun BoutLineChart(
    chart: ChartData,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    showOpponents: Boolean = false,
) {
    val colors = LocalSumoColors.current
    val textMeasurer = rememberTextMeasurer()
    val latestOnSelect by rememberUpdatedState(onSelect)
    val tapModifier = if (onSelect == null) Modifier else Modifier.pointerInput(chart) {
        detectTapGestures { pos ->
            val scale = size.width / 340f
            val hit = chart.dots.withIndex().minByOrNull { (_, d) ->
                val dx = d.x * scale - pos.x
                val dy = d.y * scale - pos.y
                dx * dx + dy * dy
            } ?: return@detectTapGestures
            val dx = hit.value.x * scale - pos.x
            val dy = hit.value.y * scale - pos.y
            // Generous radius: dots are small and fingers aren't.
            if (sqrt(dx * dx + dy * dy) <= 28.dp.toPx()) latestOnSelect?.invoke(hit.index)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(340f / 150f)
            .then(tapModifier)
    ) {
        val scale = size.width / 340f
        fun px(v: Float) = v * scale
        // 10 logical units, converted from pixels; passing (10 * scale).sp would apply
        // screen density twice and blow the labels up ~3x on a phone.
        val labelStyle = TextStyle(fontFamily = Archivo, fontSize = (10f * scale).toSp(), color = colors.faint)

        listOf(10f, 70f, 130f).forEach { gy ->
            drawLine(
                color = colors.line,
                start = Offset(px(24f), px(gy)),
                end = Offset(px(340f), px(gy)),
                strokeWidth = px(1f),
            )
        }
        listOf(10f to "10", 70f to "5", 130f to "0").forEach { (gy, label) ->
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(measured, topLeft = Offset(0f, px(gy) - measured.size.height / 2f))
        }

        if (showOpponents) {
            // Opponents' scores: muted and dashed, broken where an opponent wasn't rated.
            val dash = PathEffect.dashPathEffect(floatArrayOf(px(4f), px(3f)))
            var prev: Offset? = null
            chart.oppPoints.forEach { p ->
                val pt = p?.let { Offset(px(it.x), px(it.y)) }
                if (pt != null && prev != null) {
                    drawLine(colors.faint, prev!!, pt, strokeWidth = px(1.5f), pathEffect = dash, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                prev = pt
            }
            chart.oppPoints.filterNotNull().forEach { p ->
                drawCircle(color = colors.faint, radius = px(2.5f), center = Offset(px(p.x), px(p.y)))
            }
        }

        if (chart.points.size >= 2) {
            val path = Path()
            chart.points.forEachIndexed { i, p ->
                val pt = Offset(px(p.x), px(p.y))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = path,
                color = colors.ink,
                style = Stroke(
                    width = px(2f),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                ),
            )
        }

        chart.dots.getOrNull(selected ?: -1)?.let { d ->
            drawLine(
                color = colors.faint,
                start = Offset(px(d.x), px(10f)),
                end = Offset(px(d.x), px(130f)),
                strokeWidth = px(1f),
            )
        }

        chart.dots.forEachIndexed { i, d ->
            val center = Offset(px(d.x), px(d.y))
            if (i == selected) {
                drawCircle(color = colors.ink, radius = px(7f), center = center, style = Stroke(width = px(1.5f)))
            }
            val fill = if (d.won == false) colors.ink else colors.bg
            drawCircle(color = fill, radius = px(3.5f), center = center)
            drawCircle(color = colors.ink, radius = px(3.5f), center = center, style = Stroke(width = px(1.5f)))
        }

        if (chart.first.isNotEmpty()) {
            val measured = textMeasurer.measure(chart.first, labelStyle)
            drawText(measured, topLeft = Offset(px(24f), size.height - measured.size.height))
        }
        if (chart.last.isNotEmpty()) {
            val measured = textMeasurer.measure(chart.last, labelStyle)
            drawText(measured, topLeft = Offset(size.width - measured.size.width, size.height - measured.size.height))
        }
    }
}
