package com.aquanoodle.sumorater.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.aquanoodle.sumorater.sumoController
import com.aquanoodle.sumorater.ui.bubble.BubbleService
import com.aquanoodle.sumorater.data.allRatingsCsv
import com.aquanoodle.sumorater.data.allRatingsFileName
import com.aquanoodle.sumorater.data.bashoList
import com.aquanoodle.sumorater.data.bashoLabel
import com.aquanoodle.sumorater.data.copyPhotoToInternalStorage
import com.aquanoodle.sumorater.data.rikishiCsv
import com.aquanoodle.sumorater.data.rikishiFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aquanoodle.sumorater.ui.screens.DetailScreen
import com.aquanoodle.sumorater.ui.screens.MenuScreen
import com.aquanoodle.sumorater.ui.screens.RateScreen
import com.aquanoodle.sumorater.ui.screens.StartScreen
import com.aquanoodle.sumorater.ui.screens.TableScreen
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.SumoRaterTheme
import kotlinx.coroutines.launch

@Composable
fun SumoRaterApp() {
    val context = LocalContext.current
    val controller = remember(context) { context.sumoController }
    val state = controller.state
    val scope = rememberCoroutineScope()

    var pendingPhotoTarget by remember { mutableStateOf<Int?>(null) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingPhotoTarget
        pendingPhotoTarget = null
        if (uri != null && target != null) {
            scope.launch {
                val path = copyPhotoToInternalStorage(context, uri, target)
                if (path != null) controller.setPhoto(target, path)
            }
        }
    }
    val requestPhoto: (Int) -> Unit = { rikishiId ->
        pendingPhotoTarget = rikishiId
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = pendingCsv
        pendingCsv = null
        if (uri != null && csv != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        // BOM so Excel/Sheets read accented names and notes as UTF-8.
                        context.contentResolver.openOutputStream(uri)?.use { it.write(("﻿" + csv).toByteArray()) } != null
                    }.getOrDefault(false)
                }
                Toast.makeText(context, if (ok) "Ratings exported" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val exportCsv: (String, String) -> Unit = { fileName, csv ->
        pendingCsv = csv
        createCsv.launch(fileName)
    }

    val openBubble: () -> Unit = {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(
                context,
                "Allow \"Display over other apps\" for Sumo Rater, then tap the bubble button again.",
                Toast.LENGTH_LONG,
            ).show()
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        } else {
            BubbleService.start(context)
            // Step aside so the video app is right there with the bubble on top.
            (context as? Activity)?.moveTaskToBack(true)
        }
    }

    (context as? ComponentActivity)?.let { activity ->
        LaunchedEffect(state.isDark) {
            val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            insets.isAppearanceLightStatusBars = !state.isDark
            insets.isAppearanceLightNavigationBars = !state.isDark
        }
    }

    SumoRaterTheme(darkTheme = state.isDark) {
        val colors = LocalSumoColors.current
        if (!state.initialized) {
            Box(modifier = Modifier.fillMaxSize().background(colors.bg), contentAlignment = Alignment.Center) {
                Text("", color = colors.bg)
            }
            return@SumoRaterTheme
        }

        when (state.screen) {
            Screen.START -> StartScreen(
                bashoLabel = bashoLabel(state.bashoId),
                bashoList = remember { bashoList() },
                day = state.day,
                ratedRikishi = controller.perRikishiMap().size,
                onPickBasho = controller::pickBasho,
                onPickDay = controller::pickDay,
                onStart = controller::enterRate,
                onOpenTable = controller::openTable,
            )

            Screen.RATE -> RateScreen(
                bouts = state.bouts,
                idx = state.idx,
                day = state.day,
                bashoLabel = bashoLabel(state.bashoId),
                loading = state.loading,
                east = state.east,
                west = state.west,
                note = state.note,
                ratedToday = state.bouts.count { state.ratings[it.key]?.hasScore == true },
                photos = state.photos,
                onOpenMenu = controller::openMenu,
                onOpenTable = controller::openTable,
                onEastSettled = controller::onEastSettled,
                onWestSettled = controller::onWestSettled,
                onNoteChange = controller::onNoteChange,
                onNext = controller::save,
                onBack = controller::back,
                onRestartDay = { controller.jumpTo(0) },
                onPhotoTap = requestPhoto,
                onOpenBubble = openBubble,
            )

            Screen.MENU -> MenuScreen(
                ratedRikishi = controller.perRikishiMap().size,
                bashoLabel = bashoLabel(state.bashoId),
                bashoList = remember { bashoList() },
                day = state.day,
                isDark = state.isDark,
                onClose = controller::openRate,
                onOpenTable = controller::openTable,
                onPickBasho = controller::pickBasho,
                onPickDay = controller::pickDay,
                onToggleTheme = controller::toggleTheme,
                onExportCsv = { exportCsv(allRatingsFileName(), allRatingsCsv(state.ratings)) },
            )

            Screen.TABLE -> TableScreen(
                rows = controller.perRikishiMap().values.sortedByDescending { it.avgN },
                photos = state.photos,
                onBack = controller::openMenu,
                onOpenDetail = controller::openDetail,
                onExportCsv = { exportCsv(allRatingsFileName(), allRatingsCsv(state.ratings)) },
            )

            Screen.DETAIL -> {
                val aggregate = state.selected?.let { controller.perRikishiMap()[it] }
                if (aggregate != null) {
                    DetailScreen(
                        rikishi = aggregate,
                        photoPath = state.photos[aggregate.id],
                        chart = controller.chartFor(aggregate),
                        mode = state.mode,
                        onBack = controller::openTable,
                        onModeChange = controller::setMode,
                        onPhotoTap = { requestPhoto(aggregate.id) },
                        onEditEntry = controller::editBout,
                        onExportCsv = { exportCsv(rikishiFileName(aggregate.name), rikishiCsv(aggregate)) },
                    )
                } else {
                    androidx.compose.runtime.LaunchedEffect(state.selected) { controller.openTable() }
                }
            }
        }
    }
}
