package com.aquanoodle.sumorater.ui

import com.aquanoodle.sumorater.data.BestScope
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.BoutFilter
import com.aquanoodle.sumorater.data.ChartMode
import com.aquanoodle.sumorater.data.Rating
import com.aquanoodle.sumorater.data.TableSort
import com.aquanoodle.sumorater.data.Wrestler
import com.aquanoodle.sumorater.data.Pick
import com.aquanoodle.sumorater.data.TorikumiRow

enum class Screen { START, RATE, MENU, TABLE, DETAIL, BEST }

data class UiState(
    val screen: Screen = Screen.START,
    val idx: Int = 0,
    val east: Int? = null,
    val west: Int? = null,
    val note: String = "",
    val tags: Set<String> = emptySet(),
    /** Wrestler tags' owners (wrestler ids) for the open bout. */
    val tagOwners: Map<String, Set<Int>> = emptyMap(),
    val ratings: Map<String, Rating> = emptyMap(),
    val bouts: List<Bout> = emptyList(),
    val bashoId: String = "",
    val day: Int = 1,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val selected: Int? = null,
    val mode: ChartMode = ChartMode.DAY,
    /** Wrestler chart: also draw the opponents' scores. */
    val showOpponents: Boolean = false,
    val isDark: Boolean = false,
    val photos: Map<Int, String> = emptyMap(),
    val initialized: Boolean = false,
    /** "My ratings" search and filters. */
    val tableFilter: BoutFilter = BoutFilter(),
    val tableSort: TableSort = TableSort.SCORE,
    /** Tapping the selected sort again flips it. */
    val tableSortReversed: Boolean = false,
    val bestScope: BestScope = BestScope.BASHO,
    /** Day-15 lineups per division, offered when adding a playoff bout by hand. */
    val playoffRoster: Map<String, List<Wrestler>> = emptyMap(),
    /** Cached torikumi by "{bashoId}-{day}", for records, missing ratings and pick results. */
    val dayCaches: Map<String, Map<String, List<TorikumiRow>>> = emptyMap(),
    /** Your winner picks by bout key. */
    val picks: Map<String, Pick> = emptyMap(),
    /** A wheel was moved on the open bout: too late to pick (the wheels hint at the winner). */
    val wheelTouched: Boolean = false,
)
