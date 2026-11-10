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
import com.aquanoodle.sumorater.data.RikishiEntry
import com.aquanoodle.sumorater.data.SAMPLE
import com.aquanoodle.sumorater.data.SumoApi
import com.aquanoodle.sumorater.data.WhereState
import com.aquanoodle.sumorater.data.buildChart
import com.aquanoodle.sumorater.data.currentBashoDay
import com.aquanoodle.sumorater.data.perRikishi
import com.aquanoodle.sumorater.data.ratingKey
import com.aquanoodle.sumorater.data.toBouts
import com.aquanoodle.sumorater.data.toTorikumiRow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Highest score a side's wheel offers. Both wheels start at 10…0 so an untouched
 * bout gives nothing away; only once the winner has a score does the loser's
 * wheel shrink to below it (the original rating rule).
 */
fun wheelTop(bout: Bout, east: Int?, west: Int?, isEast: Boolean): Int {
    val eastWon = bout.winnerId == bout.eastId
    val winnerScore = if (eastWon) east else west
    val isLoser = isEast != eastWon
    return if (isLoser && winnerScore != null) winnerScore - 1 else 10
}

/** Wheel rows top→bottom: top, top-1 … 0, then one blank row (= not rated). */
fun wheelItems(top: Int): List<String> = (top downTo 0).map { it.toString() } + ""

fun wheelValueAt(top: Int, index: Int): Int? = if (index > top) null else (top - index).coerceIn(0, top)

fun wheelIndexOf(top: Int, value: Int?): Int = if (value == null) top + 1 else (top - value).coerceIn(0, top)

class SumoViewModel(private val repo: RatingsRepository) : ViewModel() {

    var state by mutableStateOf(UiState())
        private set

    /** Resume index per "{bashoId}-{day}", persisted alongside ratings. */
    private var pos: MutableMap<String, Int> = mutableMapOf()

    /** Bumped on every loadDay() call so a stale async result can no-op. */
    private var loadToken = 0

    init {
        viewModelScope.launch {
            val stored = repo.loadRatings()
            // Ratings used to be keyed by list position; re-key by the two wrestlers
            // so they stay attached to the right bout now that the list changed.
            val ratings = stored.values.associateBy { ratingKey(it) }
            if (ratings.keys != stored.keys) repo.saveRatings(ratings)
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
            // Deliberately no loadDay() here: the Start screen lets the user pick
            // a basho/day first, so nothing about a bout is fetched or shown
            // until they explicitly choose to rate it.
        }
    }

    /** User confirmed the basho/day shown on the Start screen; begin rating. */
    fun enterRate() {
        state = state.copy(screen = Screen.RATE)
        loadDay()
    }

    fun currentBout(): Bout? = state.bouts.getOrNull(state.idx)

    private fun cacheKey(bashoId: String = state.bashoId, day: Int = state.day) = "$bashoId-$day"

    /** Loads the day's torikumi. With [jumpKey], opens that bout instead of the resume position. */
    fun loadDay(jumpKey: String? = null) {
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
                repo.loadDayCache(key) ?: SAMPLE[key]?.mapValues { (_, rows) -> rows.map { it.toTorikumiRow() } }
            }
            if (myToken != loadToken) return@launch

            val bouts = src?.let { toBouts(bashoId, day, it) } ?: emptyList()
            val jumped = jumpKey?.let { k -> bouts.indexOfFirst { it.key == k }.takeIf { it >= 0 } }
            val idx = (jumped ?: pos[key] ?: 0).coerceIn(0, bouts.size)
            state = state.copy(bouts = bouts, idx = idx, loading = false)
            loadBout()
        }
    }

    fun loadBout() {
        val bout = currentBout()
        if (bout == null || state.screen != Screen.RATE) return
        val saved = state.ratings[bout.key]
        val eastIsWinner = saved?.winnerId == bout.eastId
        state = state.copy(
            east = saved?.let { if (eastIsWinner) it.w else it.l },
            west = saved?.let { if (eastIsWinner) it.l else it.w },
            note = saved?.note.orEmpty(),
        )
    }

    fun onEastSettled(index: Int) = onSideSettled(isEast = true, index = index)

    fun onWestSettled(index: Int) = onSideSettled(isEast = false, index = index)

    /** Winner ∈ 1–10 or blank; loser ∈ 0–(winner−1), or 0–10 while the winner is blank. */
    private fun onSideSettled(isEast: Boolean, index: Int) {
        val bout = currentBout() ?: return
        if (!bout.ratable) return
        var v = wheelValueAt(wheelTop(bout, state.east, state.west, isEast), index)
        val eastWon = bout.winnerId == bout.eastId
        if (isEast == eastWon) {
            if (v == 0) v = 1
            var loser = if (eastWon) state.west else state.east
            if (v != null && loser != null && loser >= v) loser = v - 1
            state = if (isEast) state.copy(east = v, west = loser) else state.copy(west = v, east = loser)
        } else {
            state = if (isEast) state.copy(east = v) else state.copy(west = v)
        }
    }

    fun onNoteChange(text: String) {
        state = state.copy(note = text)
    }

    /** Saves the current bout (if it can be rated) and advances to the next one. */
    fun save() {
        val bout = currentBout() ?: return
        val ratings = state.ratings.toMutableMap()
        if (bout.ratable) {
            val note = state.note.trim().ifEmpty { null }
            if (state.east == null && state.west == null && note == null) {
                ratings.remove(bout.key)
            } else {
                val eastWon = bout.winnerId == bout.eastId
                val updated = Rating(
                    bashoId = bout.bashoId,
                    day = bout.day,
                    div = bout.div,
                    winnerId = bout.winnerId,
                    winnerName = if (eastWon) bout.eastName else bout.westName,
                    winnerRank = if (eastWon) bout.eastRank else bout.westRank,
                    loserId = if (eastWon) bout.westId else bout.eastId,
                    loserName = if (eastWon) bout.westName else bout.eastName,
                    loserRank = if (eastWon) bout.westRank else bout.eastRank,
                    w = if (eastWon) state.east else state.west,
                    l = if (eastWon) state.west else state.east,
                    ts = System.currentTimeMillis(),
                    note = note,
                )
                val existing = ratings[bout.key]
                // Paging past an unchanged bout shouldn't bump its timestamp.
                if (existing == null || existing.copy(ts = 0) != updated.copy(ts = 0)) ratings[bout.key] = updated
            }
        }
        val nextIdx = minOf(state.bouts.size, state.idx + 1)
        pos[cacheKey()] = nextIdx
        val posSnapshot = pos.toMap()
        val changed = ratings != state.ratings
        viewModelScope.launch {
            if (changed) repo.saveRatings(ratings)
            repo.savePos(posSnapshot)
        }
        state = state.copy(ratings = ratings, idx = nextIdx)
        loadBout()
    }

    /** Previous bout; from the "day done" screen this reopens the last bout. */
    fun back() {
        if (state.idx <= 0) return
        state = state.copy(idx = state.idx - 1)
        loadBout()
    }

    fun jumpTo(index: Int) {
        state = state.copy(idx = index.coerceIn(0, state.bouts.size))
        loadBout()
    }

    /** Opens the bout behind a saved rating so it can be changed. */
    fun editBout(entry: RikishiEntry) {
        state = state.copy(bashoId = entry.bashoId, day = entry.day, screen = Screen.RATE, bouts = emptyList(), idx = 0)
        loadDay(jumpKey = entry.key)
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
