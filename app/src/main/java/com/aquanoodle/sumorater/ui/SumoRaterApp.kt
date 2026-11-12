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
import com.aquanoodle.sumorater.appUpdater
import com.aquanoodle.sumorater.sumoController
import com.aquanoodle.sumorater.ui.bubble.BubbleService
import com.aquanoodle.sumorater.data.BashoOption
import com.aquanoodle.sumorater.data.CsvImport
import com.aquanoodle.sumorater.data.bestBouts
import com.aquanoodle.sumorater.data.overviewRows
import com.aquanoodle.sumorater.data.kimariteOptions
import com.aquanoodle.sumorater.data.parseRatingsCsv
import com.aquanoodle.sumorater.ui.screens.BestScreen
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
    val updater = remember(context) { context.appUpdater }
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
    val importCsv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } }.getOrNull()
            }
            val message = when (val parsed = text?.let { parseRatingsCsv(it) }) {
                null -> "Couldn't read that file"
                is CsvImport.Error -> parsed.message
                is CsvImport.Ok -> {
                    val (added, updated) = controller.importRatings(parsed.ratings)
                    buildString {
                        append("Imported: $added new, $updated updated")
                        val unchanged = parsed.ratings.size - added - updated
                        if (unchanged > 0) append(", $unchanged already up to date")
                        if (parsed.skipped > 0) append(", ${parsed.skipped} unreadable rows skipped")
                    }
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                ratedDays = controller.ratedDays(state.bashoId),
                ratedRikishi = controller.perRikishiMap().size,
                onPickBasho = controller::pickBasho,
                onPickDay = controller::pickDay,
                onStart = controller::enterRate,
                onOpenTable = controller::openTable,
                updater = updater,
            )

            Screen.RATE -> RateScreen(
                bouts = state.bouts,
                idx = state.idx,
                day = state.day,
                bashoLabel = bashoLabel(state.bashoId),
                loading = state.loading,
                refreshing = state.refreshing,
                onRefresh = { controller.refreshDay { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
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
                onToggleTag = controller::toggleTag,
                tags = state.tags,
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
                ratedDays = controller.ratedDays(state.bashoId),
                onImportCsv = { importCsv.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
                onOpenBest = controller::openBest,
                onExportCsv = { exportCsv(allRatingsFileName(), allRatingsCsv(state.ratings)) },
                updater = updater,
            )

            Screen.TABLE -> TableScreen(
                rows = overviewRows(state.ratings, state.tableFilter, state.tableSort),
                photos = state.photos,
                onBack = controller::openMenu,
                onOpenDetail = controller::openDetail,
                onExportCsv = { exportCsv(allRatingsFileName(), allRatingsCsv(state.ratings)) },
                hasAnyRatings = state.ratings.values.any { it.hasScore },
                matchingBouts = if (state.tableFilter.isActive) state.ratings.values.count { it.hasScore && state.tableFilter.matches(it) } else 0,
                bashoOptions = state.ratings.values.map { it.bashoId }.distinct().sortedDescending().map { BashoOption(it, bashoLabel(it)) },
                kimariteOptions = kimariteOptions(state.ratings),
                filter = state.tableFilter,
                sort = state.tableSort,
                onFilter = controller::setTableFilter,
                onSort = controller::setTableSort,
            )

            Screen.BEST -> BestScreen(
                bouts = bestBouts(state.ratings, state.bestScope, state.bashoId, state.day),
                scope = state.bestScope,
                bashoLabel = bashoLabel(state.bashoId),
                day = state.day,
                onScope = controller::setBestScope,
                onBack = controller::openMenu,
                onOpenBout = { key, r -> controller.editBout(key, r.bashoId, r.day) },
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
                        onEditEntry = { controller.editBout(it) },
                        onExportCsv = { exportCsv(rikishiFileName(aggregate.name), rikishiCsv(aggregate)) },
                        matchingKeys = state.tableFilter.takeIf { it.isActive }?.let { f ->
                            state.ratings.filterValues(f::matches).keys
                        },
                    )
                } else {
                    androidx.compose.runtime.LaunchedEffect(state.selected) { controller.openTable() }
                }
            }
        }
    }
}
