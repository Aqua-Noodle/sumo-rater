package com.aquanoodle.sumorater.data

import java.time.Instant
import java.time.LocalDate

private fun csvField(value: Any?): String {
    val s = value?.toString() ?: ""
    return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}

private fun csvRow(vararg values: Any?): String = values.joinToString(",") { csvField(it) }

private fun divOrder(div: String) = DIVS.indexOf(div).let { if (it < 0) DIVS.size else it }

private const val TAG_SEPARATOR = "; "

/** Every saved bout, one row each, oldest basho first. Also the backup format read by [parseRatingsCsv]. */
fun allRatingsCsv(ratings: Map<String, Rating>): String {
    val rows = ratings.values.sortedWith(
        compareBy<Rating>({ it.bashoId }, { it.day }, { divOrder(it.div) }, { it.ts })
    )
    return buildString {
        appendLine(
            csvRow(
                "basho_id", "basho", "day", "division",
                "winner_id", "winner", "winner_rank", "winner_rating",
                "loser_id", "loser", "loser_rank", "loser_rating",
                "kimarite", "tags", "note", "rated_at",
            )
        )
        rows.forEach { r ->
            appendLine(
                csvRow(
                    r.bashoId, bashoLabel(r.bashoId), r.day, r.div,
                    r.winnerId, r.winnerName, r.winnerRank, r.w,
                    r.loserId, r.loserName, r.loserRank, r.l,
                    r.kimarite, r.tags.joinToString(TAG_SEPARATOR),
                    r.note, if (r.ts > 0) Instant.ofEpochMilli(r.ts).toString() else "",
                )
            )
        }
    }
}

/** One rikishi's rated bouts, chronological. */
fun rikishiCsv(agg: RikishiAggregate): String = buildString {
    appendLine(
        csvRow(
            "rikishi", "basho_id", "basho", "day", "division", "rank",
            "opponent", "result", "rating", "kimarite", "tags", "note",
        )
    )
    agg.list.forEach { e ->
        appendLine(
            csvRow(
                agg.name, e.bashoId, bashoLabel(e.bashoId), e.day, e.div, e.rank,
                e.opp, if (e.won) "win" else "loss", e.rating, e.kimarite, e.tags.joinToString(TAG_SEPARATOR), e.note,
            )
        )
    }
}

fun allRatingsFileName(): String = "sumo-ratings-${LocalDate.now()}.csv"

fun rikishiFileName(name: String): String =
    "sumo-ratings-${name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}.csv"

/** RFC 4180-style parser: quoted fields may contain commas, quotes ("") and line breaks. */
private fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0
    val s = text.removePrefix("﻿")
    while (i < s.length) {
        val c = s[i]
        if (inQuotes) {
            if (c == '"') {
                if (i + 1 < s.length && s[i + 1] == '"') {
                    field.append('"'); i++
                } else {
                    inQuotes = false
                }
            } else if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') {
                // Line break inside a note that went through Windows/Excel: keep it as \n.
            } else {
                field.append(c)
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field.clear() }
                '\r' -> Unit
                '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = mutableListOf() }
                else -> field.append(c)
            }
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row)
    }
    return rows.filter { r -> r.any { it.isNotBlank() } }
}

sealed interface CsvImport {
    data class Ok(val ratings: List<Rating>, val skipped: Int) : CsvImport
    data class Error(val message: String) : CsvImport
}

/** Reads a file written by [allRatingsCsv]. */
fun parseRatingsCsv(text: String): CsvImport {
    val rows = parseCsv(text)
    if (rows.isEmpty()) return CsvImport.Error("The file is empty.")
    val header = rows.first().map { it.trim().lowercase() }
    val col = header.withIndex().associate { (i, name) -> name to i }
    val required = listOf("basho_id", "day", "division", "winner_id", "loser_id")
    if (required.any { it !in col }) {
        return if ("winner" in col && "winner_id" !in col) {
            CsvImport.Error("This export is from an older version without wrestler IDs, so it can't be imported. Exports made from now on can.")
        } else {
            CsvImport.Error("This isn't a Sumo Rater \"all ratings\" export.")
        }
    }
    var skipped = 0
    val out = rows.drop(1).mapNotNull { r ->
        fun get(name: String): String = col[name]?.let { r.getOrNull(it) }?.trim().orEmpty()
        val bashoId = get("basho_id")
        val day = get("day").toIntOrNull()
        val winnerId = get("winner_id").toIntOrNull()
        val loserId = get("loser_id").toIntOrNull()
        if (bashoId.length != 6 || day == null || winnerId == null || loserId == null) {
            skipped++
            return@mapNotNull null
        }
        Rating(
            bashoId = bashoId,
            day = day,
            div = get("division"),
            winnerId = winnerId,
            winnerName = get("winner"),
            winnerRank = get("winner_rank"),
            loserId = loserId,
            loserName = get("loser"),
            loserRank = get("loser_rank"),
            w = get("winner_rating").toIntOrNull()?.coerceIn(1, 10),
            l = get("loser_rating").toIntOrNull()?.coerceIn(0, 10),
            ts = runCatching { Instant.parse(get("rated_at")).toEpochMilli() }.getOrDefault(0L),
            note = get("note").ifEmpty { null },
            kimarite = get("kimarite").ifEmpty { null },
            tags = get("tags").split(";").map { it.trim() }.filter { it.isNotEmpty() },
        )
    }
    return CsvImport.Ok(out, skipped)
}
