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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquanoodle.sumorater.data.bashoList
import com.aquanoodle.sumorater.data.bashoLabel
import com.aquanoodle.sumorater.data.copyPhotoToInternalStorage
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
    val viewModel: SumoViewModel = viewModel(factory = SumoViewModel.factory(context))
    val state = viewModel.state
    val scope = rememberCoroutineScope()

    var pendingPhotoTarget by remember { mutableStateOf<Int?>(null) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingPhotoTarget
        pendingPhotoTarget = null
        if (uri != null && target != null) {
            scope.launch {
                val path = copyPhotoToInternalStorage(context, uri, target)
                if (path != null) viewModel.setPhoto(target, path)
            }
        }
    }
    val requestPhoto: (Int) -> Unit = { rikishiId ->
        pendingPhotoTarget = rikishiId
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    (context as? ComponentActivity)?.let { activity ->
        LaunchedEffect(state.isDark) {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = !state.isDark
            controller.isAppearanceLightNavigationBars = !state.isDark
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
                ratedRikishi = viewModel.perRikishiMap().size,
                onPickBasho = viewModel::pickBasho,
                onPickDay = viewModel::pickDay,
                onStart = viewModel::enterRate,
                onOpenTable = viewModel::openTable,
            )

            Screen.RATE -> RateScreen(
                bouts = state.bouts,
                idx = state.idx,
                day = state.day,
                bashoLabel = bashoLabel(state.bashoId),
                loading = state.loading,
                w = state.w,
                l = state.l,
                photos = state.photos,
                onOpenMenu = viewModel::openMenu,
                onWinnerSettled = viewModel::onWinnerSettled,
                onLoserSettled = viewModel::onLoserSettled,
                onNext = { viewModel.save(true) },
                onBack = viewModel::back,
                onPhotoTap = requestPhoto,
            )

            Screen.MENU -> MenuScreen(
                ratedRikishi = viewModel.perRikishiMap().size,
                bashoLabel = bashoLabel(state.bashoId),
                bashoList = remember { bashoList() },
                day = state.day,
                isDark = state.isDark,
                onClose = viewModel::openRate,
                onOpenTable = viewModel::openTable,
                onPickBasho = viewModel::pickBasho,
                onPickDay = viewModel::pickDay,
                onToggleTheme = viewModel::toggleTheme,
            )

            Screen.TABLE -> TableScreen(
                rows = viewModel.perRikishiMap().values.sortedByDescending { it.avgN },
                photos = state.photos,
                onBack = viewModel::openMenu,
                onOpenDetail = viewModel::openDetail,
            )

            Screen.DETAIL -> {
                val aggregate = state.selected?.let { viewModel.perRikishiMap()[it] }
                if (aggregate != null) {
                    DetailScreen(
                        rikishi = aggregate,
                        photoPath = state.photos[aggregate.id],
                        chart = viewModel.chartFor(aggregate),
                        mode = state.mode,
                        onBack = viewModel::openTable,
                        onModeChange = viewModel::setMode,
                        onPhotoTap = { requestPhoto(aggregate.id) },
                    )
                } else {
                    androidx.compose.runtime.LaunchedEffect(state.selected) { viewModel.openTable() }
                }
            }
        }
    }
}
