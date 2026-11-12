package com.aquanoodle.sumorater.data

import java.util.Locale

/** Derives per-rikishi rating history from the flat ratings map, skipping blank sides. */
fun perRikishi(ratings: Map<String, Rating>): Map<Int, RikishiAggregate> {
    class Acc(var name: String, val list: MutableList<RikishiEntry> = mutableListOf())

    val map = LinkedHashMap<Int, Acc>()
    fun add(key: String, r: Rating, id: Int, name: String, rank: String, oppId: Int, opp: String, won: Boolean, rating: Int?) {
        if (rating == null) return
        val acc = map.getOrPut(id) { Acc(name) }
        acc.name = name
        acc.list.add(
            RikishiEntry(
                key = key, bashoId = r.bashoId, day = r.day, div = r.div,
                oppId = oppId, opp = opp, won = won, rating = rating, rank = rank,
                note = r.note, kimarite = r.kimarite, tags = r.tags,
            )
        )
    }

    ratings.forEach { (key, r) ->
        add(key, r, r.winnerId, r.winnerName, r.winnerRank, r.loserId, r.loserName, true, r.w)
        add(key, r, r.loserId, r.loserName, r.loserRank, r.winnerId, r.winnerName, false, r.l)
    }

    return map.mapValues { (id, acc) ->
        val sorted = acc.list.sortedBy { it.bashoId + it.day.toString().padStart(2, '0') }
        val count = sorted.size
        val avgN = sorted.sumOf { it.rating }.toDouble() / count
        RikishiAggregate(
            id = id,
            name = acc.name,
            rank = sorted.last().rank,
            list = sorted,
            avgN = avgN,
            avg = String.format(Locale.US, "%.1f", avgN),
            count = count,
        )
    }
}

enum class TableSort(val label: String) { SCORE("Score"), RANK("Rank"), NAME("Name"), BOUTS("Bouts") }

private val RANK_ORDER = listOf("Y", "O", "S", "K", "M", "J")
private val SHORT_RANK = Regex("^([A-Z])(\\d*)([ew]?)$")

/** Sort key for a short rank like "M4e": Yokozuna first, then by number, east before west. */
fun rankSortKey(rank: String): Int {
    val m = SHORT_RANK.find(rank) ?: return Int.MAX_VALUE
    val tier = RANK_ORDER.indexOf(m.groupValues[1]).let { if (it < 0) RANK_ORDER.size else it }
    val num = m.groupValues[2].toIntOrNull() ?: 0
    val side = if (m.groupValues[3] == "w") 1 else 0
    return tier * 10_000 + num * 10 + side
}

/** The "My ratings" overview: averages over only the bouts matching the filters. */
fun overviewRows(
    ratings: Map<String, Rating>,
    bashoId: String?,
    div: String?,
    sort: TableSort,
): List<RikishiAggregate> {
    val filtered = ratings.filterValues { r -> (bashoId == null || r.bashoId == bashoId) && (div == null || r.div == div) }
    val rows = perRikishi(filtered).values
    return when (sort) {
        TableSort.SCORE -> rows.sortedWith(compareByDescending<RikishiAggregate> { it.avgN }.thenByDescending { it.count })
        TableSort.RANK -> rows.sortedWith(compareBy<RikishiAggregate> { rankSortKey(it.rank) }.thenBy { it.name })
        TableSort.NAME -> rows.sortedBy { it.name.lowercase() }
        TableSort.BOUTS -> rows.sortedWith(compareByDescending<RikishiAggregate> { it.count }.thenByDescending { it.avgN })
    }
}

enum class BestScope(val label: String) { DAY("Day"), BASHO("Basho"), ALL("All time") }

/** Bouts ranked by combined (winner + loser) score; ties go to the more recent rating. */
fun bestBouts(ratings: Map<String, Rating>, scope: BestScope, bashoId: String, day: Int): List<Pair<String, Rating>> =
    ratings.entries
        .filter { (_, r) ->
            r.hasScore && when (scope) {
                BestScope.DAY -> r.bashoId == bashoId && r.day == day
                BestScope.BASHO -> r.bashoId == bashoId
                BestScope.ALL -> true
            }
        }
        .sortedWith(compareByDescending<Map.Entry<String, Rating>> { it.value.total }.thenByDescending { it.value.ts })
        .map { it.key to it.value }
