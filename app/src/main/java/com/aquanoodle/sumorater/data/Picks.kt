package com.aquanoodle.sumorater.data

import kotlinx.serialization.Serializable

/** Your guess of who wins a bout, made before rating it. Can't be changed once made. */
@Serializable
data class Pick(
    val bashoId: String,
    val day: Int,
    val div: String,
    val eastId: Int,
    val westId: Int,
    val pickedId: Int,
    val ts: Long,
)

/**
 * Whether [p] was right: true/false once the result is known (from your rating, or the
 * cached torikumi), null while it isn't, or when the bout ended without a fought result.
 */
fun pickOutcome(p: Pick, key: String, ratings: Map<String, Rating>, days: (String, Int) -> Map<String, List<TorikumiRow>>?): Boolean? {
    ratings[key]?.takeIf { !it.pending }?.let { return it.winnerId == p.pickedId }
    val rows = days(p.bashoId, p.day)?.get(p.div) ?: return null
    val row = rows.firstOrNull { setOf(it.eastId, it.westId) == setOf(p.eastId, p.westId) } ?: return null
    val k = row.kimarite.trim().lowercase()
    if (k.startsWith("fusen") || k in setOf("hikiwake", "itamiwake", "azukari", "mushobu")) return null
    if (row.winnerId != p.eastId && row.winnerId != p.westId) return null
    return row.winnerId == p.pickedId
}

/** Right / decided picks, optionally only bouts involving [wrestlerId]. */
data class PickScore(val right: Int, val decided: Int)

fun pickScore(
    picks: Map<String, Pick>,
    ratings: Map<String, Rating>,
    days: (String, Int) -> Map<String, List<TorikumiRow>>?,
    wrestlerId: Int? = null,
): PickScore {
    val outcomes = picks.filter { (_, p) -> wrestlerId == null || p.eastId == wrestlerId || p.westId == wrestlerId }
        .mapNotNull { (key, p) -> pickOutcome(p, key, ratings, days) }
    return PickScore(right = outcomes.count { it }, decided = outcomes.size)
}
