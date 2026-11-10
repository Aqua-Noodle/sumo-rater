package com.aquanoodle.sumorater.data

import java.time.Instant
import java.time.LocalDate

private fun csvField(value: Any?): String {
    val s = value?.toString() ?: ""
    return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}

private fun csvRow(vararg values: Any?): String = values.joinToString(",") { csvField(it) }

private fun divOrder(div: String) = DIVS.indexOf(div).let { if (it < 0) DIVS.size else it }

/** Every saved bout, one row each, oldest basho first. */
fun allRatingsCsv(ratings: Map<String, Rating>): String {
    val rows = ratings.values.sortedWith(
        compareBy<Rating>({ it.bashoId }, { it.day }, { divOrder(it.div) }, { it.ts })
    )
    return buildString {
        appendLine(
            csvRow(
                "basho_id", "basho", "day", "division",
                "winner", "winner_rank", "winner_rating",
                "loser", "loser_rank", "loser_rating",
                "note", "rated_at",
            )
        )
        rows.forEach { r ->
            appendLine(
                csvRow(
                    r.bashoId, bashoLabel(r.bashoId), r.day, r.div,
                    r.winnerName, r.winnerRank, r.w,
                    r.loserName, r.loserRank, r.l,
                    r.note, if (r.ts > 0) Instant.ofEpochMilli(r.ts).toString() else "",
                )
            )
        }
    }
}

/** One rikishi's rated bouts, chronological. */
fun rikishiCsv(agg: RikishiAggregate): String = buildString {
    appendLine(csvRow("rikishi", "basho_id", "basho", "day", "division", "rank", "opponent", "result", "rating", "note"))
    agg.list.forEach { e ->
        appendLine(
            csvRow(
                agg.name, e.bashoId, bashoLabel(e.bashoId), e.day, e.div, e.rank,
                e.opp, if (e.won) "win" else "loss", e.rating, e.note,
            )
        )
    }
}

fun allRatingsFileName(): String = "sumo-ratings-${LocalDate.now()}.csv"

fun rikishiFileName(name: String): String =
    "sumo-ratings-${name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}.csv"
