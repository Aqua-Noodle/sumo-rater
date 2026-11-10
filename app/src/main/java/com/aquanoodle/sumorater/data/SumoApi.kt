package com.aquanoodle.sumorater.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin client for sumo-api.com (free, no key). Returns null on any failure so
 * callers can fall back to bundled sample data / cache.
 */
object SumoApi {

    suspend fun fetchTorikumi(bashoId: String, division: String, day: Int): List<TorikumiRow>? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.sumo-api.com/api/basho/$bashoId/torikumi/$division/$day")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val body = try {
                    if (conn.responseCode !in 200..299) return@withContext null
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
                parseTorikumi(body)
            } catch (e: Exception) {
                null
            }
        }

    /** Every bout of the day in official order, east/west as listed; unfought and fusen bouts included. */
    private fun parseTorikumi(body: String): List<TorikumiRow> {
        val root = JSONObject(body)
        val arr = root.optJSONArray("torikumi") ?: return emptyList()
        val out = mutableListOf<Pair<Int, TorikumiRow>>()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val winnerId = if (m.isNull("winnerId")) 0 else m.optInt("winnerId", 0)
            val kimarite = if (m.isNull("kimarite")) "" else m.optString("kimarite")
            val order = m.optInt("matchNo", 0).takeIf { it > 0 } ?: (i + 1)
            out.add(
                order to TorikumiRow(
                    eastId = m.optInt("eastId"),
                    eastName = m.optString("eastShikona"),
                    eastRank = m.optString("eastRank"),
                    westId = m.optInt("westId"),
                    westName = m.optString("westShikona"),
                    westRank = m.optString("westRank"),
                    winnerId = winnerId,
                    kimarite = kimarite,
                )
            )
        }
        return out.sortedBy { it.first }.map { it.second }
    }
}
