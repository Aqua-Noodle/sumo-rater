package com.aquanoodle.sumorater.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.ChartMode
import com.aquanoodle.sumorater.data.RatingsRepository
import com.aquanoodle.sumorater.data.Rating
import com.aquanoodle.sumorater.data.RikishiAggregate
import com.aquanoodle.sumorater.data.RikishiEntry
import com.aquanoodle.sumorater.data.SAMPLE
import com.aquanoodle.sumorater.data.SumoApi
import com.aquanoodle.sumorater.data.WhereState
import com.aquanoodle.sumorater.data.currentBashoDay
import com.aquanoodle.sumorater.data.perRikishi
import com.aquanoodle.sumorater.data.ratingKey
import com.aquanoodle.sumorater.data.resolvePreRating
import com.aquanoodle.sumorater.data.isBlankRating
import com.aquanoodle.sumorater.data.migrateConflict
import com.aquanoodle.sumorater.data.toBouts
import com.aquanoodle.sumorater.data.makuuchiStart
import com.aquanoodle.sumorater.data.DaySlot
import com.aquanoodle.sumorater.data.Pick
import com.aquanoodle.sumorater.data.PickScore
import com.aquanoodle.sumorater.data.wrestlerSlots
import com.aquanoodle.sumorater.data.LAST_REGULAR_DAY
import com.aquanoodle.sumorater.data.ManualBout
import com.aquanoodle.sumorater.data.PLAYOFF_DAY
import com.aquanoodle.sumorater.data.Wrestler
import com.aquanoodle.sumorater.data.asPending
import com.aquanoodle.sumorater.data.boutKey
import com.aquanoodle.sumorater.data.playoffBouts
import com.aquanoodle.sumorater.data.playoffSource
import com.aquanoodle.sumorater.data.rosterFrom
import com.aquanoodle.sumorater.data.toTorikumiRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.aquanoodle.sumorater.data.TorikumiRow
import com.aquanoodle.sumorater.data.BestScope
import com.aquanoodle.sumorater.data.BoutFilter
import com.aquanoodle.sumorater.data.QUICK_TAGS
import com.aquanoodle.sumorater.data.TagScope
import com.aquanoodle.sumorater.data.nextTagOwners
import com.aquanoodle.sumorater.data.tagScope
import com.aquanoodle.sumorater.data.TableSort
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Highest score a side's wheel offers. Both wheels start at 10…0 so an untouched
 * bout gives nothing away; only once the winner has a score does the loser's
 * wheel shrink to below it (the original rating rule).
 *
 * Pre-ratings (no result yet) are free on both sides. A resolved pre-rating that
 * conflicts keeps the loser's wheel at 10…0 so the scores you gave stay visible
 * until you fix them.
 */
fun wheelTop(bout: Bout, east: Int?, west: Int?, isEast: Boolean): Int {
    if (bout.awaitingResult) return 10
    val eastWon = bout.winnerId == bout.eastId
    val winnerScore = if (eastWon) east else west
    val loserScore = if (eastWon) west else east
    val isLoser = isEast != eastWon
    val consistent = loserScore == null || (winnerScore != null && loserScore < winnerScore)
    return if (isLoser && winnerScore != null && winnerScore >= 1 && consistent) winnerScore - 1 else 10
}

/** Wheel rows top→bottom: top, top-1 … 0, then one blank row (= not rated). */
fun wheelItems(top: Int): List<String> = (top downTo 0).map { it.toString() } + ""

fun wheelValueAt(top: Int, index: Int): Int? = if (index > top) null else (top - index).coerceIn(0, top)

fun wheelIndexOf(top: Int, value: Int?): Int = if (value == null) top + 1 else (top - value).coerceIn(0, top)

/**
 * App-wide rating state, owned by [com.aquanoodle.sumorater.SumoApplication] so the
 * main screen and the floating bubble work on the same bout at the same time.
 */
class SumoController(private val repo: RatingsRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var state by mutableStateOf(UiState())
        private set

    /** Resume index per "{bashoId}-{day}", persisted alongside ratings. */
    private var pos: MutableMap<String, Int> = mutableMapOf()

    /** Bumped on every loadDay() call so a stale async result can no-op. */
    private var loadToken = 0

    init {
        scope.launch {
            val stored = repo.loadRatings()
            // Ratings used to be keyed by list position; re-key by the two wrestlers
            // so they stay attached to the right bout now that the list changed.
            val ratings = stored.values.map(::migrateConflict).filter { !it.isBlankRating }.associateBy { ratingKey(it) }
            if (ratings != stored) repo.saveRatings(ratings)
            pos = repo.loadPos().toMutableMap()
            val theme = repo.loadTheme()
            val where = repo.loadWhere() ?: currentBashoDay().let { WhereState(it.bashoId, it.day) }
            val photos = repo.loadPhotos()
            manualBouts = repo.loadManualBouts()
            val dayCaches = repo.loadAllDayCaches()
            val picks = repo.loadPicks()
            state = state.copy(
                ratings = ratings,
                isDark = theme == "dark",
                bashoId = where.bashoId,
                day = where.day,
                photos = photos,
                dayCaches = dayCaches,
                picks = picks,
                initialized = true,
            )
            // Deliberately no loadDay() here: the Start screen lets the user pick
            // a basho/day first, so nothing about a bout is fetched or shown
            // until they explicitly choose to rate it.
            resolvePendingInBackground()
        }
    }

    /** User confirmed the basho/day shown on the Start screen; begin rating. */
    fun enterRate() {
        record(Screen.RATE)
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
        scope.launch { repo.saveWhere(WhereState(bashoId, day)) }
        val myToken = ++loadToken
        state = state.copy(loading = true, bouts = emptyList())
        scope.launch {
            val fetched = fetchDay(bashoId, day)
            if (myToken != loadToken) return@launch

            val src = if (fetched.src != null) {
                storeDay(key, fetched.src)
                fetched.src
            } else {
                repo.loadDayCache(key) ?: SAMPLE[key]?.mapValues { (_, rows) -> rows.map { it.toTorikumiRow() } }
            }
            if (myToken != loadToken) return@launch

            val bouts = boutsFor(bashoId, day, src)
            if (day == PLAYOFF_DAY) {
                state = state.copy(playoffRoster = rosterFrom(repo.loadDayCache(cacheKey(bashoId, LAST_REGULAR_DAY))))
            }
            val jumped = jumpKey?.let { k -> bouts.indexOfFirst { it.key == k }.takeIf { it >= 0 } }
            val idx = (jumped ?: pos[key] ?: 0).coerceIn(0, bouts.size)
            state = state.copy(bouts = bouts, idx = idx, loading = false)
            backfillKimarite(bouts)
            resolvePending(bouts)
            loadBout()
        }
    }

    /** Ratings saved before kimarite were recorded pick it up once their day is loaded. */
    private fun backfillKimarite(bouts: List<Bout>) {
        val missing = bouts.mapNotNull { b ->
            val r = state.ratings[b.key] ?: return@mapNotNull null
            if (r.kimarite == null && b.ratable && b.kimarite.isNotBlank()) b.key to r.copy(kimarite = b.kimarite) else null
        }
        if (missing.isEmpty()) return
        val ratings = state.ratings + missing
        state = state.copy(ratings = ratings)
        scope.launch { repo.saveRatings(ratings) }
    }

    /**
     * Pre-ratings among [bouts] whose result is now out become normal ratings (scores
     * moved to winner/loser, contradictions flagged). Returns how many new conflicts.
     */
    private fun resolvePending(bouts: List<Bout>): Int {
        val resolved = bouts.mapNotNull { b ->
            val r = state.ratings[b.key] ?: return@mapNotNull null
            resolvePreRating(r, b)?.let { b.key to it }
        }
        if (resolved.isEmpty()) return 0
        // A fusen bout pre-rated with scores only leaves nothing behind.
        val ratings = (state.ratings + resolved).filterValues { !it.isBlankRating }
        state = state.copy(ratings = ratings)
        scope.launch { repo.saveRatings(ratings) }
        return resolved.count { it.second.conflict != null }
    }

    /**
     * Checks days that still have pre-ratings (e.g. rated live, app closed before the
     * results came out) so their conflicts show up in the calendar. Nothing is shown
     * about the bouts themselves.
     */
    private fun resolvePendingInBackground() {
        val days = state.ratings.values.filter { it.pending }
            .map { it.bashoId to it.day }.distinct()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.first }.thenByDescending { it.second })
            .take(10)
        if (days.isEmpty()) return
        scope.launch {
            days.forEach { (bashoId, day) ->
                val src = fetchDay(bashoId, day).src ?: return@forEach
                storeDay(cacheKey(bashoId, day), src)
                resolvePending(boutsFor(bashoId, day, src))
            }
        }
    }

    private class DayFetch(val reachable: Boolean, val src: Map<String, List<TorikumiRow>>?)

    /** [DayFetch.src] is null when the server couldn't be reached or has nothing for that day yet. */
    /** Saves a fetched day and keeps it in memory for records, timelines and picks. */
    private suspend fun storeDay(key: String, src: Map<String, List<TorikumiRow>>) {
        repo.saveDayCache(key, src)
        state = state.copy(dayCaches = state.dayCaches + (key to src))
    }

    /** Last regular day of [bashoId] you've rated anything on: records and timelines stop there (no spoilers). */
    fun furthestRatedDay(bashoId: String): Int =
        state.ratings.values.filter { it.bashoId == bashoId && it.day <= LAST_REGULAR_DAY }.maxOfOrNull { it.day } ?: 0

    private val fetchingDays = HashSet<String>()

    /** Fetches days up to [furthestRatedDay] that aren't cached yet, so records and timelines are complete. */
    fun ensureDays(bashoIds: Collection<String>) {
        val missing = bashoIds.flatMap { b -> (1..furthestRatedDay(b)).map { b to it } }
            .filter { (b, d) -> cacheKey(b, d) !in state.dayCaches && cacheKey(b, d) !in fetchingDays }
        if (missing.isEmpty()) return
        missing.forEach { (b, d) -> fetchingDays.add(cacheKey(b, d)) }
        scope.launch {
            missing.forEach { (b, d) ->
                fetchTorikumiDay(b, d).src?.let { storeDay(cacheKey(b, d), it) }
                fetchingDays.remove(cacheKey(b, d))
            }
        }
    }

    /** [id]'s days in [bashoIds] (up to the furthest day you've rated in each). */
    fun slotsFor(id: Int, bashoIds: Collection<String>): List<DaySlot> =
        bashoIds.sorted().flatMap { b ->
            wrestlerSlots(id, b, furthestRatedDay(b)) { d -> state.dayCaches[cacheKey(b, d)] }
        }

    /** Your picks' score, overall or in bouts involving [wrestlerId]. */
    fun pickScore(wrestlerId: Int? = null): PickScore =
        com.aquanoodle.sumorater.data.pickScore(state.picks, state.ratings, { b, d -> state.dayCaches[cacheKey(b, d)] }, wrestlerId)

    /** A pick is possible until the bout has a rating or a wheel was moved; one per bout, final. */
    fun canPick(): Boolean {
        val bout = currentBout() ?: return false
        return bout.canRate && bout.key !in state.picks && bout.key !in state.ratings && !state.wheelTouched
    }

    fun pick(wrestlerId: Int) {
        val bout = currentBout() ?: return
        if (!canPick() || (wrestlerId != bout.eastId && wrestlerId != bout.westId)) return
        val p = Pick(bout.bashoId, bout.day, bout.div, bout.eastId, bout.westId, wrestlerId, System.currentTimeMillis())
        val picks = state.picks + (bout.key to p)
        state = state.copy(picks = picks)
        scope.launch { repo.savePicks(picks) }
    }

    /** Playoff bouts added by hand, all basho. */
    private var manualBouts: List<ManualBout> = emptyList()

    /** The day's bouts from fetched/cached torikumi; the playoff day also gets your manual bouts. */
    private fun boutsFor(bashoId: String, day: Int, src: Map<String, List<TorikumiRow>>?): List<Bout> =
        if (day == PLAYOFF_DAY) playoffBouts(bashoId, src, manualBouts) else src?.let { toBouts(bashoId, day, it) } ?: emptyList()

    private suspend fun fetchDay(bashoId: String, day: Int): DayFetch =
        if (day == PLAYOFF_DAY) fetchPlayoffs(bashoId) else fetchTorikumiDay(bashoId, day)

    /**
     * The API's playoff bouts, wherever it lists them (extra day-15 entries or a day 16).
     * Day 15 is cached on the way, which also feeds the wrestler list for manual bouts.
     */
    private suspend fun fetchPlayoffs(bashoId: String): DayFetch = coroutineScope {
        val d15 = async { fetchTorikumiDay(bashoId, LAST_REGULAR_DAY) }
        val d16 = async { fetchTorikumiDay(bashoId, PLAYOFF_DAY) }
        val day15 = d15.await()
        val day16 = d16.await()
        day15.src?.let { storeDay(cacheKey(bashoId, LAST_REGULAR_DAY), it) }
        val src = playoffSource(day15.src, day16.src)
        DayFetch(day15.reachable, src.ifEmpty { null })
    }

    private suspend fun fetchTorikumiDay(bashoId: String, day: Int): DayFetch = coroutineScope {
        val juryoDeferred = async { SumoApi.fetchTorikumi(bashoId, "Juryo", day) }
        val makuuchiDeferred = async { SumoApi.fetchTorikumi(bashoId, "Makuuchi", day) }
        val juryo = juryoDeferred.await()
        val makuuchi = makuuchiDeferred.await()
        val reachable = juryo != null && makuuchi != null
        val hasData = !juryo.isNullOrEmpty() || !makuuchi.isNullOrEmpty()
        DayFetch(reachable, if (reachable && hasData) mapOf("Juryo" to juryo!!, "Makuuchi" to makuuchi!!) else null)
    }

    /**
     * Re-fetches the open day (e.g. while a basho is live) without moving off the
     * current bout or discarding values entered but not yet saved.
     * [onResult] gets a short message for the user.
     */
    fun refreshDay(onResult: (String) -> Unit = {}) {
        if (state.refreshing || state.loading) return
        val bashoId = state.bashoId
        val day = state.day
        val token = loadToken
        state = state.copy(refreshing = true)
        scope.launch {
            val fetched = fetchDay(bashoId, day)
            if (token != loadToken || bashoId != state.bashoId || day != state.day) {
                state = state.copy(refreshing = false)
                return@launch
            }
            val src = fetched.src
            if (src == null) {
                state = state.copy(refreshing = false)
                onResult(if (fetched.reachable) "No results published yet" else "Couldn't reach the server")
                return@launch
            }
            storeDay(cacheKey(bashoId, day), src)
            val hadBouts = state.bouts.isNotEmpty()
            val ratableBefore = state.bouts.filter { it.ratable }.map { it.key }.toSet()
            val currentKey = currentBout()?.key
            val bouts = boutsFor(bashoId, day, src)
            val idx = currentKey?.let { k -> bouts.indexOfFirst { it.key == k }.takeIf { it >= 0 } }
                ?: state.idx.coerceIn(0, bouts.size)
            val sameBout = currentKey != null && bouts.getOrNull(idx)?.key == currentKey
            state = state.copy(bouts = bouts, idx = idx, refreshing = false)
            backfillKimarite(bouts)
            val conflicts = resolvePending(bouts)
            if (!sameBout) loadBout()
            val newResults = bouts.count { it.ratable && it.key !in ratableBefore }
            val message = when {
                !hadBouts -> "Bouts loaded"
                newResults == 0 -> "No new results yet"
                newResults == 1 -> "1 new result"
                else -> "$newResults new results"
            }
            onResult(
                when (conflicts) {
                    0 -> message
                    1 -> "$message · 1 pre-rating conflicts"
                    else -> "$message · $conflicts pre-ratings conflict"
                }
            )
        }
    }

    fun loadBout() {
        val bout = currentBout()
        // No screen check: the bubble keeps rating while the app shows another screen.
        if (bout == null) return
        val saved = state.ratings[bout.key]
        val eastIsWinner = saved?.winnerId == bout.eastId
        state = state.copy(
            east = saved?.let { if (eastIsWinner) it.w else it.l },
            west = saved?.let { if (eastIsWinner) it.l else it.w },
            note = saved?.note.orEmpty(),
            tags = saved?.tags.orEmpty().toSet(),
            tagOwners = saved?.tagOwners.orEmpty().mapValues { it.value.toSet() },
            wheelTouched = false,
        )
    }

    /**
     * Shared and bout tags switch on/off. Wrestler tags (Henka, Injury, …) cycle
     * east → west → both → off, so one tap says whose it is.
     */
    fun toggleTag(tag: String) {
        val bout = currentBout()
        if (bout == null || tagScope(tag) != TagScope.WRESTLER) {
            state = state.copy(tags = if (tag in state.tags) state.tags - tag else state.tags + tag)
            return
        }
        val next = if (tag in state.tags) nextTagOwners(state.tagOwners[tag].orEmpty(), bout.eastId, bout.westId) else setOf(bout.eastId)
        state = if (next == null) {
            state.copy(tags = state.tags - tag, tagOwners = state.tagOwners - tag)
        } else {
            state.copy(tags = state.tags + tag, tagOwners = state.tagOwners + (tag to next))
        }
    }

    /** A wrestler tag dropped onto one of the two wrestlers: it's theirs (switched on if it wasn't). */
    fun assignTag(tag: String, wrestlerId: Int) {
        if (tagScope(tag) != TagScope.WRESTLER) return
        state = state.copy(tags = state.tags + tag, tagOwners = state.tagOwners + (tag to setOf(wrestlerId)))
    }

    fun onEastSettled(index: Int) = onSideSettled(isEast = true, index = index)

    fun onWestSettled(index: Int) = onSideSettled(isEast = false, index = index)

    /** Winner ∈ 1–10 or blank; loser ∈ 0–(winner−1), or 0–10 while the winner is blank. */
    private fun onSideSettled(isEast: Boolean, index: Int) {
        val bout = currentBout() ?: return
        if (!bout.canRate) return
        state = state.copy(wheelTouched = true)
        var v = wheelValueAt(wheelTop(bout, state.east, state.west, isEast), index)
        if (bout.awaitingResult) {
            // Pre-rating: no winner known yet, so both sides are free 0–10.
            state = if (isEast) state.copy(east = v) else state.copy(west = v)
            return
        }
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
        commit(bout, ratings)
        val nextIdx = minOf(state.bouts.size, state.idx + 1)
        pos[cacheKey()] = nextIdx
        val posSnapshot = pos.toMap()
        val changed = ratings != state.ratings
        scope.launch {
            if (changed) repo.saveRatings(ratings)
            repo.savePos(posSnapshot)
        }
        state = state.copy(ratings = ratings, idx = nextIdx)
        loadBout()
    }

    /** Writes the wheels/note/tags of [bout] into [ratings] (removing it when everything is blank). */
    private fun commit(bout: Bout, ratings: MutableMap<String, Rating>) {
        if (bout.canRate) {
            val note = state.note.trim().ifEmpty { null }
            val tags = QUICK_TAGS.filter { it in state.tags }
            val tagOwners = tags.filter { tagScope(it) == TagScope.WRESTLER }.mapNotNull { tag ->
                state.tagOwners[tag]?.filter { it == bout.eastId || it == bout.westId }?.sorted()?.takeIf { it.isNotEmpty() }?.let { tag to it }
            }.toMap()
            if (state.east == null && state.west == null && note == null && tags.isEmpty()) {
                ratings.remove(bout.key)
            } else {
                // Pre-rating: east goes in the winner fields until the result is known.
                val pending = bout.awaitingResult
                val eastWon = pending || bout.winnerId == bout.eastId
                val w = if (eastWon) state.east else state.west
                val l = if (eastWon) state.west else state.east
                // A conflict stays marked until its scores are actually changed, so paging
                // past a bout doesn't silently clear it.
                val before = ratings[bout.key]
                val keepConflict = !pending && before?.conflict != null && before.w == w && before.l == l
                val updated = Rating(
                    bashoId = bout.bashoId,
                    day = bout.day,
                    div = bout.div,
                    winnerId = if (eastWon) bout.eastId else bout.westId,
                    winnerName = if (eastWon) bout.eastName else bout.westName,
                    winnerRank = if (eastWon) bout.eastRank else bout.westRank,
                    loserId = if (eastWon) bout.westId else bout.eastId,
                    loserName = if (eastWon) bout.westName else bout.eastName,
                    loserRank = if (eastWon) bout.westRank else bout.eastRank,
                    w = w,
                    l = l,
                    ts = System.currentTimeMillis(),
                    note = note,
                    kimarite = bout.kimarite.ifBlank { null },
                    tags = tags,
                    tagOwners = tagOwners,
                    pending = pending,
                    conflict = if (keepConflict) before?.conflict else null,
                )
                val existing = ratings[bout.key]
                // Paging past an unchanged bout shouldn't bump its timestamp.
                if (existing == null || existing.copy(ts = 0) != updated.copy(ts = 0)) ratings[bout.key] = updated
            }
        }
    }

    /** Adds a playoff bout by hand (playoff day only) and opens it. */
    fun addPlayoffBout(div: String, east: Wrestler, west: Wrestler) {
        if (state.day != PLAYOFF_DAY || east.id == west.id) return
        val bashoId = state.bashoId
        val row = TorikumiRow(east.id, east.name, east.rank, west.id, west.name, west.rank)
        val added = ManualBout(bashoId, div, row)
        val key = boutKey(bashoId, PLAYOFF_DAY, div, east.id, west.id)
        if (state.bouts.none { it.key == key }) {
            manualBouts = manualBouts + added
            val snapshot = manualBouts
            scope.launch { repo.saveManualBouts(snapshot) }
            rebuildPlayoffs()
        }
        state.bouts.indexOfFirst { it.key == key }.takeIf { it >= 0 }?.let { jumpTo(it) }
    }

    /** Removes the current hand-added playoff bout and its rating. */
    fun removePlayoffBout() {
        val bout = currentBout()?.takeIf { it.manual } ?: return
        manualBouts = manualBouts.filterNot { it.matches(bout) }
        val snapshot = manualBouts
        val ratings = state.ratings - bout.key
        state = state.copy(ratings = ratings)
        scope.launch {
            repo.saveManualBouts(snapshot)
            repo.saveRatings(ratings)
        }
        rebuildPlayoffs()
        jumpTo(state.idx)
    }

    /**
     * Sets (or clears, with null) who won the current hand-added playoff bout. Scores
     * already entered are kept and re-matched to the new winner, like a pre-rating.
     */
    fun setPlayoffWinner(winnerId: Int?) {
        val bout = currentBout()?.takeIf { it.manual } ?: return
        val ratings = state.ratings.toMutableMap()
        commit(bout, ratings)
        manualBouts = manualBouts.map { if (it.matches(bout)) it.copy(row = it.row.copy(winnerId = winnerId ?: 0)) else it }
        val snapshot = manualBouts
        rebuildPlayoffs()
        val updated = state.bouts.firstOrNull { it.key == bout.key }
        ratings[bout.key]?.let { r ->
            val pending = r.asPending(bout)
            ratings[bout.key] = updated?.let { resolvePreRating(pending, it) } ?: pending
        }
        state = state.copy(ratings = ratings)
        scope.launch {
            repo.saveManualBouts(snapshot)
            repo.saveRatings(ratings)
        }
        loadBout()
    }

    private fun ManualBout.matches(bout: Bout) =
        bashoId == bout.bashoId && div == bout.div &&
            setOf(row.eastId, row.westId) == setOf(bout.eastId, bout.westId)

    /** Re-merges hand-added bouts into the open playoff day, keeping published ones as they are. */
    private fun rebuildPlayoffs() {
        if (state.day != PLAYOFF_DAY) return
        val published = state.bouts.filter { !it.manual }
        val keys = published.map { it.key }.toSet()
        val own = playoffBouts(state.bashoId, null, manualBouts).filter { it.key !in keys }
        state = state.copy(bouts = published + own)
    }

    /** Conflicting pre-ratings on the open day. */
    fun conflictsToday(): Int = state.bouts.count { state.ratings[it.key]?.conflict != null }

    /** Jumps to the next bout (after the current one, wrapping) whose pre-rating conflicts. */
    fun nextConflict() {
        val n = state.bouts.size
        if (n == 0) return
        val next = (1..n).map { (state.idx + it) % n }.firstOrNull { state.ratings[state.bouts[it].key]?.conflict != null } ?: return
        jumpTo(next)
    }

    /** Previous bout; from the "day done" screen this reopens the last bout. */
    fun back() {
        if (state.idx <= 0) return
        state = state.copy(idx = state.idx - 1)
        loadBout()
    }

    /** Saves whatever is entered on the open bout, then jumps to the first Makuuchi bout of the day. */
    fun jumpToMakuuchi() {
        val target = makuuchiStart(state.bouts) ?: return
        val bout = currentBout()
        if (bout != null) {
            val ratings = state.ratings.toMutableMap()
            commit(bout, ratings)
            if (ratings != state.ratings) {
                state = state.copy(ratings = ratings)
                scope.launch { repo.saveRatings(ratings) }
            }
        }
        pos[cacheKey()] = target
        val posSnapshot = pos.toMap()
        scope.launch { repo.savePos(posSnapshot) }
        jumpTo(target)
    }

    fun jumpTo(index: Int) {
        state = state.copy(idx = index.coerceIn(0, state.bouts.size))
        loadBout()
    }

    /** Opens the bout behind a saved rating so it can be changed. */
    fun editBout(entry: RikishiEntry) = editBout(entry.key, entry.bashoId, entry.day)

    fun editBout(key: String, bashoId: String, day: Int) {
        record(Screen.RATE, push = true)
        state = state.copy(bashoId = bashoId, day = day, screen = Screen.RATE, bouts = emptyList(), idx = 0)
        loadDay(jumpKey = key)
    }

    fun openBest() {
        record(Screen.BEST)
        state = state.copy(screen = Screen.BEST)
    }

    fun setBestScope(scope: BestScope) {
        state = state.copy(bestScope = scope)
    }

    fun setTableFilter(filter: BoutFilter) {
        state = state.copy(tableFilter = filter)
    }

    /** A new sort starts in its natural order; the same one again flips the direction. */
    fun setTableSort(sort: TableSort) {
        state = if (sort == state.tableSort) {
            state.copy(tableSortReversed = !state.tableSortReversed)
        } else {
            state.copy(tableSort = sort, tableSortReversed = false)
        }
    }

    /** Days of [bashoId] with a pre-rating that contradicts the result: red calendar dot. */
    fun conflictDays(bashoId: String): Set<Int> =
        state.ratings.values.filter { it.bashoId == bashoId && it.conflict != null }.map { it.day }.toSet()

    /** Days of [bashoId] with at least one scored bout, for the calendar's rated markers. */
    fun ratedDays(bashoId: String): Set<Int> =
        state.ratings.values.filter { it.bashoId == bashoId && it.hasScore }.map { it.day }.toSet()

    /**
     * Merges imported ratings: new bouts are added; for bouts already on the phone
     * the more recently rated version wins. Returns (added, updated).
     */
    fun importRatings(imported: List<Rating>): Pair<Int, Int> {
        val ratings = state.ratings.toMutableMap()
        var added = 0
        var updated = 0
        imported.forEach { r ->
            val key = ratingKey(r)
            val existing = ratings[key]
            when {
                existing == null -> { ratings[key] = r; added++ }
                r.ts > existing.ts && r.copy(ts = 0) != existing.copy(ts = 0) -> { ratings[key] = r; updated++ }
            }
        }
        if (added + updated > 0) {
            state = state.copy(ratings = ratings)
            scope.launch { repo.saveRatings(ratings) }
            loadBout()
        }
        return added to updated
    }

    fun openMenu() {
        record(Screen.MENU)
        state = state.copy(screen = Screen.MENU)
    }

    fun openTable() {
        record(Screen.TABLE)
        state = state.copy(screen = Screen.TABLE)
    }

    /** Next/previous wrestler on the open wrestler page: same history entry, so back still returns to the list. */
    fun switchDetail(rikishiId: Int) {
        state = state.copy(selected = rikishiId)
    }

    fun openDetail(rikishiId: Int) {
        record(Screen.DETAIL, selected = rikishiId)
        state = state.copy(screen = Screen.DETAIL, selected = rikishiId)
    }

    // Screen history, so the back button / edge swipe goes one step back instead of closing the app.
    private data class NavEntry(val screen: Screen, val selected: Int?)

    private val history = ArrayList<NavEntry>()

    private fun entryOf(screen: Screen, selected: Int?) = NavEntry(screen, if (screen == Screen.DETAIL) selected else null)

    /**
     * Notes the move from the current screen to [screen]. Going to a screen already in the
     * history unwinds back to it (menu → pick a day → rating doesn't pile up); [push] always
     * stacks, for drill-downs like editing a bout from a wrestler page.
     */
    private fun record(screen: Screen, selected: Int? = state.selected, push: Boolean = false) {
        val current = entryOf(state.screen, state.selected)
        val target = entryOf(screen, selected)
        if (current == target) return
        val at = if (push) -1 else history.indexOfLast { it == target }
        if (at >= 0) {
            while (history.size > at) history.removeAt(history.size - 1)
        } else {
            history.add(current)
            if (history.size > 40) history.removeAt(0)
        }
    }

    /** The screen that back would return to. */
    fun previousScreen(): Screen? = history.lastOrNull()?.screen

    /** One step back; the start screen is the root, where back leaves the app as usual. */
    fun navigateBack() {
        val prev = if (history.isEmpty()) entryOf(Screen.START, null) else history.removeAt(history.size - 1)
        state = state.copy(screen = prev.screen, selected = prev.selected ?: state.selected)
        if (prev.screen == Screen.RATE && state.bouts.isEmpty() && !state.loading) loadDay()
    }

    fun pickBasho(bashoId: String) = go(bashoId = bashoId, day = 1)

    fun pickDay(day: Int) = go(day = day)

    private fun go(bashoId: String = state.bashoId, day: Int) {
        record(Screen.RATE)
        state = state.copy(bashoId = bashoId, day = day, screen = Screen.RATE, bouts = emptyList(), idx = 0)
        loadDay()
    }

    fun toggleTheme() {
        val dark = !state.isDark
        scope.launch { repo.saveTheme(if (dark) "dark" else "light") }
        state = state.copy(isDark = dark)
    }

    fun toggleOpponents() {
        state = state.copy(showOpponents = !state.showOpponents)
    }

    fun setMode(mode: ChartMode) {
        state = state.copy(mode = mode)
    }

    fun setPhoto(rikishiId: Int, path: String) {
        scope.launch { repo.savePhoto(rikishiId, path) }
        state = state.copy(photos = state.photos + (rikishiId to path))
    }

    fun perRikishiMap(): Map<Int, RikishiAggregate> = perRikishi(state.ratings)
}
