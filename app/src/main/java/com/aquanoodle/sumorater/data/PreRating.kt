package com.aquanoodle.sumorater.data

/**
 * Why winner score [w] and loser score [l] break the rating rule (winner 1–10, loser below
 * the winner), or null when they're fine. Blank sides never conflict.
 */
fun scoreConflict(winnerName: String, loserName: String, w: Int?, l: Int?): String? = when {
    w == 0 -> "$winnerName won, but you gave $winnerName 0."
    w != null && l != null && l > w -> "$winnerName won, but you rated $loserName higher ($l vs $w)."
    w != null && l != null && l == w -> "$winnerName won, but you rated both $w."
    else -> null
}

/**
 * A rating whose scores contradict the result loses the offending score: the loser's when
 * it isn't below the winner's, the winner's when it is 0. [Rating.conflict] says what
 * happened and keeps the bout marked until it's rated again. Consistent ratings pass through.
 */
fun clearConflict(r: Rating): Rating {
    val reason = scoreConflict(r.winnerName, r.loserName, r.w, r.l) ?: return r
    return if (r.w == 0) {
        r.copy(w = null, conflict = "$reason That score was cleared.")
    } else {
        r.copy(l = null, conflict = "$reason ${r.loserName}'s score was cleared.")
    }
}

/** True when nothing is left worth keeping (no score, note or tag). */
val Rating.isBlankRating: Boolean get() = !hasScore && note.isNullOrBlank() && tags.isEmpty()

/**
 * Upgrades conflicts saved by the first pre-rating version (scores kept as they were) to
 * the cleared form; fusen/no-decision ones simply lose their scores.
 */
fun migrateConflict(r: Rating): Rating = when {
    r.conflict == null -> r
    r.conflict?.startsWith("Result:") == true -> r.copy(w = null, l = null, conflict = null)
    scoreConflict(r.winnerName, r.loserName, r.w, r.l) != null -> clearConflict(r.copy(conflict = null))
    else -> r
}

/**
 * Turns a pre-rating into a normal rating once [bout]'s result is out: scores move from
 * east/west to winner/loser, and a score that contradicts the result is cleared (see
 * [clearConflict]). On a fusen/no-decision bout the scores are dropped; notes and tags stay.
 * Returns null while the result is still missing (or [r] isn't a pre-rating).
 */
fun resolvePreRating(r: Rating, bout: Bout): Rating? {
    if (!r.pending || bout.awaitingResult) return null
    // While pending, the winner fields hold east and the loser fields west.
    val eastScore = if (r.winnerId == bout.eastId) r.w else r.l
    val westScore = if (r.winnerId == bout.eastId) r.l else r.w
    val westWon = bout.winnerId == bout.westId
    val oriented = r.copy(
        winnerId = if (westWon) bout.westId else bout.eastId,
        winnerName = if (westWon) bout.westName else bout.eastName,
        winnerRank = if (westWon) bout.westRank else bout.eastRank,
        loserId = if (westWon) bout.eastId else bout.westId,
        loserName = if (westWon) bout.eastName else bout.westName,
        loserRank = if (westWon) bout.eastRank else bout.westRank,
        w = if (westWon) westScore else eastScore,
        l = if (westWon) eastScore else westScore,
        kimarite = bout.kimarite.ifBlank { null },
        pending = false,
    )
    if (bout.unratableReason != null) return oriented.copy(w = null, l = null, conflict = null)
    return clearConflict(oriented.copy(conflict = null))
}

/**
 * The same scores as a pre-rating again (east in the winner fields, west in the loser
 * fields), e.g. when the winner of a hand-added playoff bout is changed.
 */
fun Rating.asPending(bout: Bout): Rating {
    val eastIsWinner = winnerId == bout.eastId
    return copy(
        winnerId = bout.eastId, winnerName = bout.eastName, winnerRank = bout.eastRank,
        loserId = bout.westId, loserName = bout.westName, loserRank = bout.westRank,
        w = if (eastIsWinner) w else l,
        l = if (eastIsWinner) l else w,
        kimarite = null,
        pending = true,
        conflict = null,
    )
}
