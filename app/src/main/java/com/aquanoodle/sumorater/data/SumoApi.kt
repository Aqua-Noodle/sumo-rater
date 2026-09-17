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

    suspend fun fetchTorikumi(bashoId: String, division: String, day: Int): List<RawBoutRow>? =
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

    private fun parseTorikumi(body: String): List<RawBoutRow> {
        val root = JSONObject(body)
        val arr = root.optJSONArray("torikumi") ?: return emptyList()
        val out = mutableListOf<RawBoutRow>()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            val winnerId = if (m.has("winnerId") && !m.isNull("winnerId")) m.optInt("winnerId", -1) else -1
            if (winnerId <= 0) continue // not yet fought
            val eastId = m.optInt("eastId")
            val westId = m.optInt("westId")
            val eastShikona = m.optString("eastShikona")
            val westShikona = m.optString("westShikona")
            val eastRank = m.optString("eastRank")
            val westRank = m.optString("westRank")
            out.add(
                if (winnerId == eastId) {
                    RawBoutRow(eastId, eastShikona, eastRank, westId, westShikona, westRank)
                } else {
                    RawBoutRow(westId, westShikona, westRank, eastId, eastShikona, eastRank)
                }
            )
        }
        return out
    }
}
