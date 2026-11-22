package com.aquanoodle.sumorater.data

/** One day of a wrestler's basho as listed in the torikumi. */
sealed interface DaySlot {
    val bashoId: String
    val day: Int
}

/** A scheduled bout. [won] is null until decided; [fusen] = default win/loss, not actually fought. */
data class FoughtSlot(
    override val bashoId: String,
    override val day: Int,
    val div: String,
    val key: String,
    val oppId: Int,
    val oppName: String,
    val won: Boolean?,
    val fusen: Boolean,
    val noDecision: Boolean,
) : DaySlot

/** Not on the day's torikumi at all (kyujo) although ranked in Juryo or Makuuchi. */
data class AbsentSlot(override val bashoId: String, override val day: Int) : DaySlot

/**
 * The days 1..[upTo] of [bashoId] for wrestler [id], from cached torikumi ([days] returns
 * a day's divisions or null if that day isn't cached; unknown days are skipped). Absence
 * is only reported for sekitori, since lower-ranked visitors don't fight every day.
 */
fun wrestlerSlots(id: Int, bashoId: String, upTo: Int, days: (Int) -> Map<String, List<TorikumiRow>>?): List<DaySlot> {
    var sekitori = false
    val found = (1..upTo.coerceAtMost(LAST_REGULAR_DAY)).mapNotNull { day ->
        val src = days(day) ?: return@mapNotNull null
        val hit = DIVS.firstNotNullOfOrNull { div ->
            val rows = src[div].orEmpty().let { if (day == LAST_REGULAR_DAY) splitPlayoffs(it).first else it }
            rows.firstOrNull { it.eastId == id || it.westId == id }?.let { div to it }
        }
        if (hit == null) return@mapNotNull day to null
        val (div, row) = hit
        val isEast = row.eastId == id
        if (rankDivision(shortRank(if (isEast) row.eastRank else row.westRank)) != null) sekitori = true
        val oppId = if (isEast) row.westId else row.eastId
        val k = row.kimarite.trim().lowercase()
        day to FoughtSlot(
            bashoId = bashoId,
            day = day,
            div = div,
            key = boutKey(bashoId, day, div, row.eastId, row.westId),
            oppId = oppId,
            oppName = if (isEast) row.westName else row.eastName,
            won = when (row.winnerId) { id -> true; oppId -> false; else -> null },
            fusen = k.startsWith("fusen"),
            noDecision = k in setOf("hikiwake", "itamiwake", "azukari", "mushobu"),
        )
    }
    return found.mapNotNull { (day, slot) -> slot ?: if (sekitori) AbsentSlot(bashoId, day) else null }
}

/** Official-style record: fusen count as wins/losses, absences separately. */
data class Record(val wins: Int, val losses: Int, val absent: Int) {
    val text: String get() = if (absent > 0) "$wins–$losses–$absent" else "$wins–$losses"
}

fun recordOf(slots: List<DaySlot>): Record = Record(
    wins = slots.count { it is FoughtSlot && it.won == true },
    losses = slots.count { it is FoughtSlot && it.won == false },
    absent = slots.count { it is AbsentSlot },
)

/** What the Days chart and the bout list show, in order. */
sealed interface TimelineItem {
    val bashoId: String
    val day: Int
}

data class RatedItem(val entry: RikishiEntry) : TimelineItem {
    override val bashoId get() = entry.bashoId
    override val day get() = entry.day
}

/** Fought and decided, but you haven't rated it. */
data class MissingItem(val slot: FoughtSlot) : TimelineItem {
    override val bashoId get() = slot.bashoId
    override val day get() = slot.day
}

/** Fusen (either way) or absent: a cross in the chart. [slot] is null when absent. */
data class OffItem(override val bashoId: String, override val day: Int, val slot: FoughtSlot?) : TimelineItem

/**
 * Rated bouts merged with the torikumi: unrated decided bouts become [MissingItem], fusen
 * and absences [OffItem]. Rated bouts without a cached day (and playoffs) stay as they are.
 */
fun wrestlerTimeline(entries: List<RikishiEntry>, slots: List<DaySlot>): List<TimelineItem> {
    val rated = entries.associateBy { it.key }
    val fromSlots = slots.mapNotNull { s ->
        when {
            s is AbsentSlot -> OffItem(s.bashoId, s.day, null)
            s is FoughtSlot && s.fusen -> OffItem(s.bashoId, s.day, s)
            s is FoughtSlot && s.key in rated -> RatedItem(rated.getValue(s.key))
            s is FoughtSlot && s.won != null && !s.noDecision -> MissingItem(s)
            else -> null
        }
    }
    val covered = fromSlots.filterIsInstance<RatedItem>().map { it.entry.key }.toSet()
    val rest = entries.filter { it.key !in covered }.map { RatedItem(it) }
    return (fromSlots + rest).sortedWith(compareBy<TimelineItem>({ it.bashoId }, { it.day }))
}
