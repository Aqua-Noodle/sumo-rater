package com.aquanoodle.sumorater.data

data class ChartPoint(val x: Float, val y: Float)
data class ChartDot(val x: Float, val y: Float, val won: Boolean?)
data class ChartData(val points: List<ChartPoint>, val dots: List<ChartDot>, val first: String, val last: String)

enum class ChartMode { DAY, BASHO }

/** 340x150 logical units; y axis 0/5/10, x from 24 to 340. Mirrors the prototype's chart(). */
fun buildChart(list: List<RikishiEntry>, mode: ChartMode): ChartData {
    data class Pt(val label: String, val v: Double, val won: Boolean?)

    val pts: List<Pt> = if (mode == ChartMode.BASHO) {
        val grouped = LinkedHashMap<String, MutableList<Int>>()
        list.forEach { grouped.getOrPut(it.bashoId) { mutableListOf() }.add(it.rating) }
        grouped.keys.sorted().map { k ->
            val ratings = grouped.getValue(k)
            Pt(bashoLabel(k), ratings.average(), null)
        }
    } else {
        list.map { Pt("${bashoLabel(it.bashoId)} d${it.day}", it.rating.toDouble(), it.won) }
    }

    val n = pts.size
    fun x(i: Int): Float = if (n < 2) 182f else 24f + i * (316f / (n - 1))
    fun y(v: Double): Float = (10 + (10 - v) * 12).toFloat()

    return ChartData(
        points = pts.mapIndexed { i, p -> ChartPoint(x(i), y(p.v)) },
        dots = pts.mapIndexed { i, p -> ChartDot(x(i), y(p.v), p.won) },
        first = pts.firstOrNull()?.label ?: "",
        last = if (n > 1) pts.last().label else "",
    )
}
