package com.aquanoodle.sumorater.data

import java.util.Locale

/** Derives per-rikishi rating history from the flat ratings map, skipping blank sides. */
fun perRikishi(ratings: Map<String, Rating>): Map<Int, RikishiAggregate> {
    class Acc(var name: String, val list: MutableList<RikishiEntry> = mutableListOf())

    val map = LinkedHashMap<Int, Acc>()
    fun add(key: String, r: Rating, id: Int, name: String, rank: String, oppId: Int, opp: String, won: Boolean, rating: Int?, oppRating: Int?) {
        if (rating == null) return
        val acc = map.getOrPut(id) { Acc(name) }
        acc.name = name
        acc.list.add(
            RikishiEntry(
                key = key, bashoId = r.bashoId, day = r.day, div = r.div,
                oppId = oppId, opp = opp, won = won, rating = rating, rank = rank,
                note = r.note, kimarite = r.kimarite, tags = r.tags,
                credited = creditedTags(r, id),
                oppRating = oppRating,
            )
        )
    }

    ratings.forEach { (key, r) ->
        // Pre-ratings don't know who won yet, so they stay out of stats until resolved.
        if (r.pending) return@forEach
        add(key, r, r.winnerId, r.winnerName, r.winnerRank, r.loserId, r.loserName, true, r.w, r.l)
        add(key, r, r.loserId, r.loserName, r.loserRank, r.winnerId, r.winnerName, false, r.l, r.w)
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

/**
 * "My ratings" search. Basho narrows the scope and division picks wrestlers by their current
 * rank (see [overviewRows]); tags (all must be set), kimarite and the keyword narrow down to
 * individual bouts. The keyword matches notes, wrestler names,
 * kimarite and tags, case- and accent-insensitively.
 */
data class BoutFilter(
    val bashoId: String? = null,
    val div: String? = null,
    val tags: Set<String> = emptySet(),
    val kimarite: String? = null,
    val query: String = "",
) {
    val isActive: Boolean get() = bashoId != null || div != null || tags.isNotEmpty() || kimarite != null || query.isNotBlank()

    fun matches(r: Rating): Boolean {
        if (bashoId != null && r.bashoId != bashoId) return false
        if (kimarite != null && !r.kimarite.equals(kimarite, ignoreCase = true)) return false
        if (!r.tags.containsAll(tags)) return false
        val words = fold(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return true
        val haystack = fold(
            listOfNotNull(r.note, r.winnerName, r.loserName, r.kimarite).joinToString(" ") +
                " " + r.tags.joinToString(" ")
        )
        return words.all { it in haystack }
    }
}

/** Lowercase without diacritics, so "ryu" also finds "Ryū". */
fun fold(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)

/** Kimarite that appear in your ratings, most common first, for the filter dropdown. */
fun kimariteOptions(ratings: Map<String, Rating>): List<String> =
    ratings.values.mapNotNull { it.kimarite?.lowercase(Locale.ROOT)?.ifBlank { null } }
        .groupingBy { it }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

/** "M4e" -> Makuuchi, "J3w" -> Juryo; null for lower divisions or unknown ranks. */
fun rankDivision(rank: String): String? {
    val m = SHORT_RANK.find(rank) ?: return null
    return when (m.groupValues[1]) {
        "Y", "O", "S", "K", "M" -> "Makuuchi"
        "J" -> "Juryo"
        else -> null
    }
}

/**
 * The "My ratings" overview: averages over only the bouts matching the filter.
 *
 * Division goes by the wrestler's rank in the selected basho, or their latest rated bout when
 * all basho are shown, so a Juryo wrestler who once was (or filled in) in Makuuchi doesn't
 * show up under Makuuchi. The row's rank is that same current rank.
 */
fun overviewRows(
    ratings: Map<String, Rating>,
    filter: BoutFilter,
    sort: TableSort,
): List<RikishiAggregate> {
    val inScope = if (filter.bashoId == null) ratings else ratings.filterValues { it.bashoId == filter.bashoId }
    val current = perRikishi(inScope).mapValues { (_, agg) ->
        val latest = agg.list.last()
        // A Makushita (or lower) rank belongs to neither division, even after a Juryo visit;
        // only a missing rank falls back to the division the bout was in.
        latest.rank to (rankDivision(latest.rank) ?: latest.div.takeIf { latest.rank.isBlank() })
    }
    val filtered = if (filter.isActive) ratings.filterValues(filter::matches) else ratings
    val rows = perRikishi(filtered).values.mapNotNull { row ->
        val (rank, division) = current[row.id] ?: return@mapNotNull null
        if (filter.div != null && division != filter.div) null else row.copy(rank = rank)
    }
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
            r.hasScore && !r.pending && when (scope) {
                BestScope.DAY -> r.bashoId == bashoId && r.day == day
                BestScope.BASHO -> r.bashoId == bashoId
                BestScope.ALL -> true
            }
        }
        .sortedWith(compareByDescending<Map.Entry<String, Rating>> { it.value.total }.thenByDescending { it.value.ts })
        .map { it.key to it.value }

/** How often each tag counts for a wrestler across [list], in tag order; tags never credited are left out. */
fun tagCounts(list: List<RikishiEntry>): List<Pair<String, Int>> {
    val counts = list.flatMap { it.credited }.groupingBy { it }.eachCount()
    return QUICK_TAGS.mapNotNull { tag -> counts[tag]?.let { tag to it } }
}
