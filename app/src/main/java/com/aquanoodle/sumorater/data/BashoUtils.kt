package com.aquanoodle.sumorater.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

val DIVS = listOf("Juryo", "Makuuchi")

private val RANKS = mapOf(
    "Yokozuna" to "Y",
    "Ozeki" to "O",
    "Sekiwake" to "S",
    "Komusubi" to "K",
    "Maegashira" to "M",
    "Juryo" to "J",
)

private val RANK_REGEX = Regex("^(\\w+)\\s*(\\d*)\\s*(East|West)?")

/** "Maegashira 4 East" -> "M4e" */
fun shortRank(rank: String?): String {
    if (rank.isNullOrEmpty()) return rank ?: ""
    val m = RANK_REGEX.find(rank) ?: return rank
    val word = m.groupValues[1]
    val num = m.groupValues[2]
    val side = m.groupValues[3]
    val prefix = RANKS[word] ?: word
    val sideChar = if (side.isNotEmpty()) side.first().lowercaseChar().toString() else ""
    return "$prefix$num$sideChar"
}

private val BASHO_NAME = mapOf(
    "01" to "Hatsu",
    "03" to "Haru",
    "05" to "Natsu",
    "07" to "Nagoya",
    "09" to "Aki",
    "11" to "Kyushu",
)

/** "202609" -> "Aki 2026" */
fun bashoLabel(id: String): String {
    val mm = id.substring(4)
    val year = id.substring(0, 4)
    return "${BASHO_NAME[mm] ?: id} $year"
}

data class BashoDay(val bashoId: String, val day: Int)

private fun secondSunday(year: Int, month: Int): LocalDate {
    val first = LocalDate.of(year, month, 1)
    val f = first.dayOfWeek.value % 7 // Sunday=0..Saturday=6, matching JS Date#getDay()
    val offset = (7 - f) % 7
    return first.plusDays((offset + 7).toLong())
}

/** Most recent basho + day for a given date: basho months 1,3,5,7,9,11; starts 2nd Sunday. */
fun currentBashoDay(d: LocalDate = LocalDate.now()): BashoDay {
    var y = d.year
    var m = d.monthValue
    if (m % 2 == 0) m -= 1
    var s = secondSunday(y, m)
    if (d.isBefore(s)) {
        m -= 2
        if (m < 1) {
            m = 11
            y -= 1
        }
        s = secondSunday(y, m)
    }
    val day = minOf(15, ChronoUnit.DAYS.between(s, d).toInt() + 1)
    return BashoDay("$y${m.toString().padStart(2, '0')}", day)
}

fun bashoList(now: BashoDay = currentBashoDay()): List<BashoOption> {
    val out = mutableListOf<BashoOption>()
    val nowYear = now.bashoId.substring(0, 4).toInt()
    for (y in nowYear downTo 1958) {
        for (mm in listOf("11", "09", "07", "05", "03", "01")) {
            val id = "$y$mm"
            if (id <= now.bashoId) out.add(BashoOption(id, bashoLabel(id)))
        }
    }
    return out
}

fun toBouts(bashoId: String, day: Int, src: Map<String, List<RawBoutRow>>): List<Bout> =
    DIVS.flatMap { div ->
        (src[div] ?: emptyList()).mapIndexed { i, b ->
            Bout(
                key = "$bashoId-$day-$div-$i",
                bashoId = bashoId,
                day = day,
                div = div,
                winnerId = b.winnerId,
                winnerName = b.winnerName,
                winnerRank = shortRank(b.winnerRank),
                loserId = b.loserId,
                loserName = b.loserName,
                loserRank = shortRank(b.loserRank),
            )
        }
    }
