package com.aquanoodle.sumorater.data

import java.util.Locale

/** Derives per-rikishi rating history from the flat ratings map, skipping blank sides. */
fun perRikishi(ratings: Map<String, Rating>): Map<Int, RikishiAggregate> {
    class Acc(var name: String, val list: MutableList<RikishiEntry> = mutableListOf())

    val map = LinkedHashMap<Int, Acc>()
    fun add(key: String, r: Rating, id: Int, name: String, rank: String, opp: String, won: Boolean, rating: Int?) {
        if (rating == null) return
        val acc = map.getOrPut(id) { Acc(name) }
        acc.name = name
        acc.list.add(
            RikishiEntry(
                key = key, bashoId = r.bashoId, day = r.day, div = r.div,
                opp = opp, won = won, rating = rating, rank = rank, note = r.note,
            )
        )
    }

    ratings.forEach { (key, r) ->
        add(key, r, r.winnerId, r.winnerName, r.winnerRank, r.loserName, true, r.w)
        add(key, r, r.loserId, r.loserName, r.loserRank, r.winnerName, false, r.l)
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
