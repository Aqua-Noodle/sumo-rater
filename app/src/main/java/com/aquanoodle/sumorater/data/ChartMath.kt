package com.aquanoodle.sumorater.data

data class ChartPoint(val x: Float, val y: Float)
data class ChartDot(val x: Float, val y: Float, val won: Boolean?)
/** [oppPoints] runs alongside [points]: the opponents' scores, null where the opponent wasn't rated. */
data class ChartData(
    val points: List<ChartPoint>,
    val dots: List<ChartDot>,
    val first: String,
    val last: String,
    val oppPoints: List<ChartPoint?> = emptyList(),
)

enum class ChartMode { DAY, BASHO }

/** 340x150 logical units; y axis 0/5/10, x from 24 to 340. Mirrors the prototype's chart(). */
fun buildChart(list: List<RikishiEntry>, mode: ChartMode): ChartData {
    data class Pt(val label: String, val v: Double, val won: Boolean?, val opp: Double?)

    val pts: List<Pt> = if (mode == ChartMode.BASHO) {
        list.groupBy { it.bashoId }.toSortedMap().map { (k, bouts) ->
            val opp = bouts.mapNotNull { it.oppRating }
            Pt(bashoLabel(k), bouts.map { it.rating }.average(), null, if (opp.isEmpty()) null else opp.average())
        }
    } else {
        list.map { Pt("${bashoLabel(it.bashoId)} ${shortDayLabel(it.day)}", it.rating.toDouble(), it.won, it.oppRating?.toDouble()) }
    }

    val n = pts.size
    fun x(i: Int): Float = if (n < 2) 182f else 24f + i * (316f / (n - 1))
    fun y(v: Double): Float = (10 + (10 - v) * 12).toFloat()

    return ChartData(
        points = pts.mapIndexed { i, p -> ChartPoint(x(i), y(p.v)) },
        dots = pts.mapIndexed { i, p -> ChartDot(x(i), y(p.v), p.won) },
        first = pts.firstOrNull()?.label ?: "",
        last = if (n > 1) pts.last().label else "",
        oppPoints = pts.mapIndexed { i, p -> p.opp?.let { ChartPoint(x(i), y(it)) } },
    )
}
