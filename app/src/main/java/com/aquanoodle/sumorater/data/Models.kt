package com.aquanoodle.sumorater.data

import kotlinx.serialization.Serializable

/**
 * One bout in official torikumi order: east on the left, west on the right.
 * [winnerId] is kept only so a rating can be stored against the right
 * wrestler; the UI never uses it to arrange or label the bout.
 */
data class Bout(
    val key: String,
    val bashoId: String,
    val day: Int,
    val div: String,
    val eastId: Int,
    val eastName: String,
    val eastRank: String,
    val westId: Int,
    val westName: String,
    val westRank: String,
    val winnerId: Int,
    val kimarite: String,
) {
    /** Null when the bout can be rated, otherwise a short reason shown instead of the wheels. */
    val unratableReason: String?
        get() {
            val k = kimarite.trim().lowercase()
            return when {
                k.startsWith("fusen") -> "Fusen"
                k in DRAW_KIMARITE -> "No decision"
                winnerId != eastId && winnerId != westId -> if (k.isEmpty()) "Not fought yet" else "No decision"
                else -> null
            }
        }

    val ratable: Boolean get() = unratableReason == null

    private companion object {
        val DRAW_KIMARITE = setOf("hikiwake", "itamiwake", "azukari", "mushobu")
    }
}

/** One torikumi entry as fetched/cached, ranks not yet shortened. */
@Serializable
data class TorikumiRow(
    val eastId: Int,
    val eastName: String,
    val eastRank: String,
    val westId: Int,
    val westName: String,
    val westRank: String,
    val winnerId: Int = 0,
    val kimarite: String = "",
)

/** Pre-1.1 cache format (winner first, decided bouts only). Read-only, for offline fallback. */
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
    val note: String? = null,
    val kimarite: String? = null,
    val tags: List<String> = emptyList(),
) {
    val hasScore: Boolean get() = w != null || l != null

    /** Combined bout score used to rank "best bouts". */
    val total: Int get() = (w ?: 0) + (l ?: 0)
}

/** One-tap labels offered under every ratable bout, in display order. */
val QUICK_TAGS = listOf("Great tachiai", "Long bout", "Close call", "Upset", "Henka", "Matta", "Monoii")

/** "yorikiri" -> "Yorikiri" */
fun kimariteLabel(k: String?): String? = k?.trim()?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercaseChar() }

@Serializable
data class WhereState(val bashoId: String, val day: Int)

data class BashoOption(val id: String, val label: String)

/** One rated appearance of a rikishi, derived from the ratings map. */
data class RikishiEntry(
    val key: String,
    val bashoId: String,
    val day: Int,
    val div: String,
    val oppId: Int,
    val opp: String,
    val won: Boolean,
    val rating: Int,
    val rank: String,
    val note: String?,
    val kimarite: String?,
    val tags: List<String>,
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
