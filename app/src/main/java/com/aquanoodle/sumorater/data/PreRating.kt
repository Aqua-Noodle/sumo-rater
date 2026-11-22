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
 * Turns a pre-rating into a normal rating once [bout]'s result is out: scores move from
 * east/west to winner/loser and any contradiction is flagged in [Rating.conflict].
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
    val reason = bout.unratableReason
    val conflict = if (reason != null) {
        if (oriented.hasScore) "Result: ${reason.lowercase()}, so this bout can't be rated." else null
    } else {
        scoreConflict(oriented.winnerName, oriented.loserName, oriented.w, oriented.l)
    }
    return oriented.copy(conflict = conflict)
}
