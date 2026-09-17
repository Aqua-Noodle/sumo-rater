package com.aquanoodle.sumorater.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.ChartData
import com.aquanoodle.sumorater.data.ChartMode
import com.aquanoodle.sumorater.data.RatingsRepository
import com.aquanoodle.sumorater.data.Rating
import com.aquanoodle.sumorater.data.RikishiAggregate
import com.aquanoodle.sumorater.data.SAMPLE
import com.aquanoodle.sumorater.data.SumoApi
import com.aquanoodle.sumorater.data.WhereState
import com.aquanoodle.sumorater.data.buildChart
import com.aquanoodle.sumorater.data.currentBashoDay
import com.aquanoodle.sumorater.data.perRikishi
import com.aquanoodle.sumorater.data.toBouts
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SumoViewModel(private val repo: RatingsRepository) : ViewModel() {

    var state by mutableStateOf(UiState())
        private set

    /** Resume index per "{bashoId}-{day}", persisted alongside ratings. */
    private var pos: MutableMap<String, Int> = mutableMapOf()

    /** Bumped on every loadDay() call so a stale async result can no-op. */
    private var loadToken = 0

    init {
        viewModelScope.launch {
            val ratings = repo.loadRatings()
            pos = repo.loadPos().toMutableMap()
            val theme = repo.loadTheme()
            val where = repo.loadWhere() ?: currentBashoDay().let { WhereState(it.bashoId, it.day) }
            val photos = repo.loadPhotos()
            state = state.copy(
                ratings = ratings,
                isDark = theme == "dark",
                bashoId = where.bashoId,
                day = where.day,
                photos = photos,
                initialized = true,
            )
            loadDay()
        }
    }

    fun currentBout(): Bout? = state.bouts.getOrNull(state.idx)

    private fun cacheKey(bashoId: String = state.bashoId, day: Int = state.day) = "$bashoId-$day"

    fun loadDay() {
        val bashoId = state.bashoId
        val day = state.day
        val key = cacheKey(bashoId, day)
        viewModelScope.launch { repo.saveWhere(WhereState(bashoId, day)) }
        val myToken = ++loadToken
        state = state.copy(loading = true, bouts = emptyList())
        viewModelScope.launch {
            val juryoDeferred = async { SumoApi.fetchTorikumi(bashoId, "Juryo", day) }
            val makuuchiDeferred = async { SumoApi.fetchTorikumi(bashoId, "Makuuchi", day) }
            val juryo = juryoDeferred.await()
            val makuuchi = makuuchiDeferred.await()
            if (myToken != loadToken) return@launch

            val src = if (juryo != null && makuuchi != null && (juryo.isNotEmpty() || makuuchi.isNotEmpty())) {
                val fetched = mapOf("Juryo" to juryo, "Makuuchi" to makuuchi)
                repo.saveDayCache(key, fetched)
                fetched
            } else {
                repo.loadDayCache(key) ?: SAMPLE[key]
            }
            if (myToken != loadToken) return@launch

            val bouts = src?.let { toBouts(bashoId, day, it) } ?: emptyList()
            val idx = pos[key] ?: 0
            state = state.copy(bouts = bouts, idx = idx, loading = false)
            loadBout()
        }
    }

    fun loadBout() {
        val bout = currentBout()
        if (bout == null || state.screen != Screen.RATE) return
        val saved = state.ratings[bout.key]
        state = state.copy(w = saved?.w, l = saved?.l)
    }

    /** Called when the winner wheel settles on row [index] (0..10, 10 = blank). */
    fun onWinnerSettled(index: Int) {
        val w = if (index >= 10) null else maxOf(1, 10 - index)
        if (w == state.w) return
        var l = state.l
        if (w != null && l != null && l >= w) l = w - 1
        state = state.copy(w = w, l = l)
    }

    /** Called when the loser wheel settles on row [index] (0..max, max = blank). */
    fun onLoserSettled(index: Int) {
        val max = state.w ?: 11
        val l = if (index >= max) null else maxOf(0, minOf(max - 1, max - 1 - index))
        if (l != state.l) state = state.copy(l = l)
    }

    /** Saves the current bout's rating and (by default) advances to the next one. */
    fun save(advance: Boolean = true) {
        val bout = currentBout() ?: return
        val ratings = state.ratings.toMutableMap()
        if (state.w == null && state.l == null) {
            ratings.remove(bout.key)
        } else {
            ratings[bout.key] = Rating(
                bashoId = bout.bashoId,
                day = bout.day,
                div = bout.div,
                winnerId = bout.winnerId,
                winnerName = bout.winnerName,
                winnerRank = bout.winnerRank,
                loserId = bout.loserId,
                loserName = bout.loserName,
                loserRank = bout.loserRank,
                w = state.w,
                l = state.l,
                ts = System.currentTimeMillis(),
            )
        }
        val nextIdx = minOf(state.bouts.size, state.idx + if (advance) 1 else 0)
        pos[cacheKey()] = nextIdx
        val posSnapshot = pos.toMap()
        viewModelScope.launch {
            repo.saveRatings(ratings)
            repo.savePos(posSnapshot)
        }
        state = state.copy(ratings = ratings, idx = nextIdx)
        loadBout()
    }

    fun back() {
        if (state.idx <= 0) return
        state = state.copy(idx = state.idx - 1)
        loadBout()
    }

    fun openMenu() {
        state = state.copy(screen = Screen.MENU)
    }

    fun openTable() {
        state = state.copy(screen = Screen.TABLE)
    }

    fun openRate() {
        state = state.copy(screen = Screen.RATE)
        loadBout()
    }

    fun openDetail(rikishiId: Int) {
        state = state.copy(screen = Screen.DETAIL, selected = rikishiId)
    }

    fun pickBasho(bashoId: String) = go(bashoId = bashoId, day = 1)

    fun pickDay(day: Int) = go(day = day)

    private fun go(bashoId: String = state.bashoId, day: Int) {
        state = state.copy(bashoId = bashoId, day = day, screen = Screen.RATE, bouts = emptyList(), idx = 0)
        loadDay()
    }

    fun toggleTheme() {
        val dark = !state.isDark
        viewModelScope.launch { repo.saveTheme(if (dark) "dark" else "light") }
        state = state.copy(isDark = dark)
    }

    fun setMode(mode: ChartMode) {
        state = state.copy(mode = mode)
    }

    fun setPhoto(rikishiId: Int, path: String) {
        viewModelScope.launch { repo.savePhoto(rikishiId, path) }
        state = state.copy(photos = state.photos + (rikishiId to path))
    }

    fun perRikishiMap(): Map<Int, RikishiAggregate> = perRikishi(state.ratings)

    fun chartFor(selected: RikishiAggregate?): ChartData =
        if (selected == null) ChartData(emptyList(), emptyList(), "", "") else buildChart(selected.list, state.mode)

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SumoViewModel(RatingsRepository(context.applicationContext)) as T
                }
            }
    }
}
