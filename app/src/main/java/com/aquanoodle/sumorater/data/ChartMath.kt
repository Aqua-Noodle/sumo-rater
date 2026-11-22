package com.aquanoodle.sumorater.data

data class ChartPoint(val x: Float, val y: Float)

/** RATED: a scored bout. MISSING: fought but not rated (hollow, on the 0 line). OFF: fusen/absent (a cross). */
enum class DotKind { RATED, MISSING, OFF }

data class ChartDot(val x: Float, val y: Float, val won: Boolean?, val kind: DotKind = DotKind.RATED)

/**
 * [points] is the line through the rated dots only. [dots] has one entry per chart item
 * (tap targets, in order). [oppPoints] runs alongside [dots]: the opponents' scores, null
 * where there's none.
 */
data class ChartData(
    val points: List<ChartPoint>,
    val dots: List<ChartDot>,
    val first: String,
    val last: String,
    val oppPoints: List<ChartPoint?> = emptyList(),
)

enum class ChartMode { DAY, BASHO }

private data class Pt(val label: String, val v: Double?, val won: Boolean?, val opp: Double?, val kind: DotKind)

/** 340x150 logical units; y axis 0/5/10, x from 24 to 340. Mirrors the prototype's chart(). */
private fun layout(pts: List<Pt>): ChartData {
    val n = pts.size
    fun x(i: Int): Float = if (n < 2) 182f else 24f + i * (316f / (n - 1))
    fun y(v: Double): Float = (10 + (10 - v) * 12).toFloat()
    return ChartData(
        points = pts.mapIndexedNotNull { i, p -> p.v?.let { ChartPoint(x(i), y(it)) } },
        dots = pts.mapIndexed { i, p -> ChartDot(x(i), y(p.v ?: 0.0), p.won, p.kind) },
        first = pts.firstOrNull()?.label ?: "",
        last = if (n > 1) pts.last().label else "",
        oppPoints = pts.mapIndexed { i, p -> p.opp?.let { ChartPoint(x(i), y(it)) } },
    )
}

fun buildChart(list: List<RikishiEntry>, mode: ChartMode): ChartData =
    if (mode == ChartMode.BASHO) {
        layout(
            list.groupBy { it.bashoId }.toSortedMap().map { (k, bouts) ->
                val opp = bouts.mapNotNull { it.oppRating }
                Pt(bashoLabel(k), bouts.map { it.rating }.average(), null, if (opp.isEmpty()) null else opp.average(), DotKind.RATED)
            }
        )
    } else {
        buildTimelineChart(list.map { RatedItem(it) })
    }

/** Days chart over a wrestler's timeline: rated bouts on the line, unrated and fusen/absent days on the 0 line. */
fun buildTimelineChart(items: List<TimelineItem>): ChartData = layout(
    items.map { item ->
        val label = "${bashoLabel(item.bashoId)} ${shortDayLabel(item.day)}"
        when (item) {
            is RatedItem -> Pt(label, item.entry.rating.toDouble(), item.entry.won, item.entry.oppRating?.toDouble(), DotKind.RATED)
            is MissingItem -> Pt(label, null, null, null, DotKind.MISSING)
            is OffItem -> Pt(label, null, null, null, DotKind.OFF)
        }
    }
)
