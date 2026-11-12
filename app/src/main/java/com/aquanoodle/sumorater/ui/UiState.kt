package com.aquanoodle.sumorater.ui

import com.aquanoodle.sumorater.data.BestScope
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.ChartMode
import com.aquanoodle.sumorater.data.Rating
import com.aquanoodle.sumorater.data.TableSort

enum class Screen { START, RATE, MENU, TABLE, DETAIL, BEST }

data class UiState(
    val screen: Screen = Screen.START,
    val idx: Int = 0,
    val east: Int? = null,
    val west: Int? = null,
    val note: String = "",
    val tags: Set<String> = emptySet(),
    val ratings: Map<String, Rating> = emptyMap(),
    val bouts: List<Bout> = emptyList(),
    val bashoId: String = "",
    val day: Int = 1,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val selected: Int? = null,
    val mode: ChartMode = ChartMode.DAY,
    val isDark: Boolean = false,
    val photos: Map<Int, String> = emptyMap(),
    val initialized: Boolean = false,
    /** "My ratings" filters; null = all. */
    val tableBasho: String? = null,
    val tableDiv: String? = null,
    val tableSort: TableSort = TableSort.SCORE,
    val bestScope: BestScope = BestScope.BASHO,
)
