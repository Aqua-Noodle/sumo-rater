package com.aquanoodle.sumorater.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.ChartData
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors

/** 340x150 logical-unit line chart, matching the detail screen's SVG exactly. */
@Composable
fun BoutLineChart(chart: ChartData, modifier: Modifier = Modifier) {
    val colors = LocalSumoColors.current
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(340f / 150f)
    ) {
        val scale = size.width / 340f
        fun px(v: Float) = v * scale
        val labelStyle = TextStyle(fontFamily = Archivo, fontSize = (10 * scale).sp, color = colors.faint)

        listOf(10f, 70f, 130f).forEach { gy ->
            drawLine(
                color = colors.line,
                start = Offset(px(24f), px(gy)),
                end = Offset(px(340f), px(gy)),
                strokeWidth = px(1f),
            )
        }
        listOf(10f to "10", 70f to "5", 130f to "0").forEach { (gy, label) ->
            drawText(textMeasurer, label, topLeft = Offset(0f, px(gy) - px(10f)), style = labelStyle)
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

        chart.dots.forEach { d ->
            val center = Offset(px(d.x), px(d.y))
            val fill = if (d.won == false) colors.ink else colors.bg
            drawCircle(color = fill, radius = px(3.5f), center = center)
            drawCircle(color = colors.ink, radius = px(3.5f), center = center, style = Stroke(width = px(1.5f)))
        }

        if (chart.first.isNotEmpty()) {
            drawText(textMeasurer, chart.first, topLeft = Offset(px(24f), px(148f) - px(10f)), style = labelStyle)
        }
        if (chart.last.isNotEmpty()) {
            val measured = textMeasurer.measure(chart.last, labelStyle)
            drawText(
                textMeasurer,
                chart.last,
                topLeft = Offset(px(340f) - measured.size.width, px(148f) - px(10f)),
                style = labelStyle,
            )
        }
    }
}
