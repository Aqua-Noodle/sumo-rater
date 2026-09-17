package com.aquanoodle.sumorater.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "sumo_rater")

/**
 * Persists everything on-device only, mirroring the prototype's localStorage
 * keys: ratings, resume positions, theme, last-viewed basho/day, and a
 * per-basho-day torikumi cache (so history stays browsable offline once
 * fetched) plus per-rikishi custom photo paths.
 */
class RatingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val RATINGS = stringPreferencesKey("sumo-rater.ratings.v1")
        val POS = stringPreferencesKey("sumo-rater.pos.v1")
        val THEME = stringPreferencesKey("sumo-rater.theme.v1")
        val WHERE = stringPreferencesKey("sumo-rater.where.v1")
        val PHOTOS = stringPreferencesKey("sumo-rater.photos.v1")
    }

    suspend fun loadRatings(): Map<String, Rating> {
        val raw = context.dataStore.data.first()[Keys.RATINGS] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Rating>>(raw) }.getOrDefault(emptyMap())
    }

    suspend fun saveRatings(ratings: Map<String, Rating>) {
        context.dataStore.edit { it[Keys.RATINGS] = json.encodeToString(ratings) }
    }

    suspend fun loadPos(): Map<String, Int> {
        val raw = context.dataStore.data.first()[Keys.POS] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(raw) }.getOrDefault(emptyMap())
    }

    suspend fun savePos(pos: Map<String, Int>) {
        context.dataStore.edit { it[Keys.POS] = json.encodeToString(pos) }
    }

    suspend fun loadTheme(): String = context.dataStore.data.first()[Keys.THEME] ?: "light"

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun loadWhere(): WhereState? {
        val raw = context.dataStore.data.first()[Keys.WHERE] ?: return null
        return runCatching { json.decodeFromString<WhereState>(raw) }.getOrNull()
    }

    suspend fun saveWhere(where: WhereState) {
        context.dataStore.edit { it[Keys.WHERE] = json.encodeToString(where) }
    }

    suspend fun loadDayCache(cacheKey: String): Map<String, List<RawBoutRow>>? {
        val key = stringPreferencesKey("sumo-rater.cache.$cacheKey")
        val raw = context.dataStore.data.first()[key] ?: return null
        return runCatching { json.decodeFromString<Map<String, List<RawBoutRow>>>(raw) }.getOrNull()
    }

    suspend fun saveDayCache(cacheKey: String, src: Map<String, List<RawBoutRow>>) {
        val key = stringPreferencesKey("sumo-rater.cache.$cacheKey")
        context.dataStore.edit { it[key] = json.encodeToString(src) }
    }

    suspend fun loadPhotos(): Map<Int, String> {
        val raw = context.dataStore.data.first()[Keys.PHOTOS] ?: return emptyMap()
        val map = runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
        return map.mapKeys { it.key.toIntOrNull() ?: 0 }.filterKeys { it != 0 }
    }

    suspend fun savePhoto(rikishiId: Int, path: String) {
        val current = loadPhotos().mapKeys { it.key.toString() }.toMutableMap()
        current[rikishiId.toString()] = path
        context.dataStore.edit { it[Keys.PHOTOS] = json.encodeToString(current) }
    }
}
