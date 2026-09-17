package com.aquanoodle.sumorater.ui

import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.ChartMode
import com.aquanoodle.sumorater.data.Rating

enum class Screen { START, RATE, MENU, TABLE, DETAIL }

data class UiState(
    val screen: Screen = Screen.START,
    val idx: Int = 0,
    val w: Int? = null,
    val l: Int? = null,
    val ratings: Map<String, Rating> = emptyMap(),
    val bouts: List<Bout> = emptyList(),
    val bashoId: String = "",
    val day: Int = 1,
    val loading: Boolean = false,
    val selected: Int? = null,
    val mode: ChartMode = ChartMode.DAY,
    val isDark: Boolean = false,
    val photos: Map<Int, String> = emptyMap(),
    val initialized: Boolean = false,
)
