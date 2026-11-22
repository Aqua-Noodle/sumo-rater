package com.aquanoodle.sumorater.data

import kotlinx.serialization.Serializable

/**
 * Playoffs (kettei-sen) are filed as an extra "day 16" of the basho, so their ratings
 * get their own keys even when the same two wrestlers also met on day 15.
 */
const val PLAYOFF_DAY = 16

const val LAST_REGULAR_DAY = 15

/** "Day 7" / "Playoff" */
fun dayLabel(day: Int): String = if (day == PLAYOFF_DAY) "Playoff" else "Day $day"

/** Compact form for chart axes: "d7" / "PO" */
fun shortDayLabel(day: Int): String = if (day == PLAYOFF_DAY) "PO" else "d$day"

/**
 * Splits a division's day-15 list into regular bouts and playoff bouts. Everyone fights
 * once a day, so a wrestler showing up again later in the list (in match order) means
 * that bout is a playoff.
 */
fun splitPlayoffs(rows: List<TorikumiRow>): Pair<List<TorikumiRow>, List<TorikumiRow>> {
    val seen = HashSet<Int>()
    val regular = mutableListOf<TorikumiRow>()
    val playoffs = mutableListOf<TorikumiRow>()
    rows.forEach { r ->
        if (r.eastId in seen || r.westId in seen) playoffs.add(r) else regular.add(r)
        seen.add(r.eastId)
        seen.add(r.westId)
    }
    return regular to playoffs
}

/**
 * Playoff bouts per division, from wherever the API lists them: extra day-15 entries
 * and/or a separate day-16 list. The same pair listed in both counts once.
 */
fun playoffSource(day15: Map<String, List<TorikumiRow>>?, day16: Map<String, List<TorikumiRow>>?): Map<String, List<TorikumiRow>> =
    DIVS.associateWith { div ->
        val extras = day15?.get(div)?.let { splitPlayoffs(it).second }.orEmpty()
        (extras + day16?.get(div).orEmpty()).distinctBy { pairOf(it.eastId, it.westId) }
    }.filterValues { it.isNotEmpty() }

private fun pairOf(a: Int, b: Int) = minOf(a, b) to maxOf(a, b)

/** A playoff bout you added yourself (before or instead of it appearing in the API). */
@Serializable
data class ManualBout(val bashoId: String, val div: String, val row: TorikumiRow)

/** A wrestler you can pick for a manual playoff bout. */
data class Wrestler(val id: Int, val name: String, val rank: String)

/** Everyone in a division's day-15 lineup, highest rank first. */
fun rosterFrom(day15: Map<String, List<TorikumiRow>>?): Map<String, List<Wrestler>> =
    DIVS.associateWith { div ->
        day15?.get(div).orEmpty()
            .flatMap { listOf(Wrestler(it.eastId, it.eastName, shortRank(it.eastRank)), Wrestler(it.westId, it.westName, shortRank(it.westRank))) }
            .distinctBy { it.id }
            .sortedWith(compareBy<Wrestler> { rankSortKey(it.rank) }.thenBy { it.name })
    }

/**
 * The playoff day's bouts: what the API published, plus your manual bouts for pairs the
 * API doesn't (yet) list. Once the API has a bout, its result replaces yours.
 */
fun playoffBouts(bashoId: String, api: Map<String, List<TorikumiRow>>?, manual: List<ManualBout>): List<Bout> {
    val published = toBouts(bashoId, PLAYOFF_DAY, api.orEmpty())
    val keys = published.map { it.key }.toSet()
    val own = manual.filter { it.bashoId == bashoId }.mapNotNull { m ->
        toBouts(bashoId, PLAYOFF_DAY, mapOf(m.div to listOf(m.row))).firstOrNull()
            ?.takeIf { it.key !in keys }
            ?.copy(manual = true)
    }
    return published + own
}
