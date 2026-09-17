package com.aquanoodle.sumorater.data

import kotlinx.serialization.Serializable

/** One bout to be rated: winner on the left, loser on the right. */
data class Bout(
    val key: String,
    val bashoId: String,
    val day: Int,
    val div: String,
    val winnerId: Int,
    val winnerName: String,
    val winnerRank: String,
    val loserId: Int,
    val loserName: String,
    val loserRank: String,
)

/** Raw (id, name, rank) x2 for winner/loser, ranks not yet shortened. */
@Serializable
data class RawBoutRow(
    val winnerId: Int,
    val winnerName: String,
    val winnerRank: String,
    val loserId: Int,
    val loserName: String,
    val loserRank: String,
)

/** A saved rating for one bout. w/l may be null (blank = unrated side). */
@Serializable
data class Rating(
    val bashoId: String,
    val day: Int,
    val div: String,
    val winnerId: Int,
    val winnerName: String,
    val winnerRank: String,
    val loserId: Int,
    val loserName: String,
    val loserRank: String,
    val w: Int? = null,
    val l: Int? = null,
    val ts: Long = 0,
)

@Serializable
data class WhereState(val bashoId: String, val day: Int)

data class BashoOption(val id: String, val label: String)

/** One rated appearance of a rikishi, derived from the ratings map. */
data class RikishiEntry(
    val bashoId: String,
    val day: Int,
    val opp: String,
    val won: Boolean,
    val rating: Int,
    val rank: String,
)

data class RikishiAggregate(
    val id: Int,
    val name: String,
    val rank: String,
    val list: List<RikishiEntry>,
    val avgN: Double,
    val avg: String,
    val count: Int,
)
