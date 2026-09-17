package com.aquanoodle.sumorater.data

import java.util.Locale

/** Derives per-rikishi rating history from the flat ratings map, skipping blank sides. */
fun perRikishi(ratings: Map<String, Rating>): Map<Int, RikishiAggregate> {
    class Acc(var name: String, val list: MutableList<RikishiEntry> = mutableListOf())

    val map = LinkedHashMap<Int, Acc>()
    fun add(id: Int, name: String, rank: String, opp: String, won: Boolean, rating: Int?, bashoId: String, day: Int) {
        if (rating == null) return
        val acc = map.getOrPut(id) { Acc(name) }
        acc.name = name
        acc.list.add(RikishiEntry(bashoId = bashoId, day = day, opp = opp, won = won, rating = rating, rank = rank))
    }

    ratings.values.forEach { r ->
        add(r.winnerId, r.winnerName, r.winnerRank, r.loserName, true, r.w, r.bashoId, r.day)
        add(r.loserId, r.loserName, r.loserRank, r.winnerName, false, r.l, r.bashoId, r.day)
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
