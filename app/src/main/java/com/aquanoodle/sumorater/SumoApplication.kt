package com.aquanoodle.sumorater

import android.app.Application
import android.content.Context
import com.aquanoodle.sumorater.data.RatingsRepository
import com.aquanoodle.sumorater.ui.SumoController

class SumoApplication : Application() {
    val controller: SumoController by lazy { SumoController(RatingsRepository(this)) }
}

val Context.sumoController: SumoController
    get() = (applicationContext as SumoApplication).controller
