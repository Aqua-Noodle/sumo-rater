package com.aquanoodle.sumorater.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.ui.components.RikishiPhoto
import com.aquanoodle.sumorater.ui.components.WheelPicker
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_THRESHOLD_DP = 70f
private const val FLY_DISTANCE_DP = 520f
private const val ENTER_OFFSET_DP = 90f

@Composable
fun RateScreen(
    bouts: List<Bout>,
    idx: Int,
    day: Int,
    bashoLabel: String,
    loading: Boolean,
    w: Int?,
    l: Int?,
    photos: Map<Int, String>,
    onOpenMenu: () -> Unit,
    onWinnerSettled: (Int) -> Unit,
    onLoserSettled: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onPhotoTap: (Int) -> Unit,
) {
    val colors = LocalSumoColors.current
    val bout = bouts.getOrNull(idx)
    val isDone = bouts.isNotEmpty() && bout == null
    val isEmpty = bouts.isEmpty()
    val canBack = idx > 0 && bout != null

    val progressLabel = if (bout != null) {
        "${bout.div} · Day $day · ${idx + 1} / ${bouts.size}".uppercase()
    } else {
        "$bashoLabel · Day $day".uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        TopBar(progressLabel = progressLabel, onOpenMenu = onOpenMenu)

        Box(modifier = Modifier.weight(1f)) {
            when {
                bout != null -> BoutCard(
                    bout = bout,
                    w = w,
                    l = l,
                    photos = photos,
                    canSwipeRight = canBack,
                    onWinnerSettled = onWinnerSettled,
                    onLoserSettled = onLoserSettled,
                    onSwipeLeft = onNext,
                    onSwipeRight = onBack,
                    onPhotoTap = onPhotoTap,
                )
                isDone -> DoneState(bashoLabel = bashoLabel, day = day, ratedCount = bouts.size)
                isEmpty -> EmptyState(
                    message = if (loading) "Loading…" else "No bout data available for $bashoLabel, day $day.",
                    onPickAnotherDay = onOpenMenu,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canBack) {
                Text(
                    text = "← Back",
                    modifier = Modifier
                        .sizeIn(minHeight = 48.dp)
                        .clickable(onClick = onBack)
                        .wrapContentSize(Alignment.CenterStart)
                        .padding(horizontal = 12.dp),
                    style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.faint),
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            if (bout != null) {
                Text(
                    text = "Next →",
                    modifier = Modifier
                        .sizeIn(minHeight = 48.dp)
                        .clickable(onClick = onNext)
                        .wrapContentSize(Alignment.CenterEnd)
                        .padding(horizontal = 12.dp),
                    style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = colors.ink),
                )
            }
        }
    }
}

@Composable
private fun TopBar(progressLabel: String, onOpenMenu: () -> Unit) {
    val colors = LocalSumoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HamburgerButton(onClick = onOpenMenu)
        Text(
            text = progressLabel,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = Archivo,
                fontSize = 12.sp,
                letterSpacing = 0.14f.em,
                color = colors.mute,
                fontFeatureSettings = TabularNums,
            ),
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun HamburgerButton(onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(colors.ink),
                )
            }
        }
    }
}

@Composable
private fun DoneState(bashoLabel: String, day: Int, ratedCount: Int) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$bashoLabel · Day $day done",
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.02f).em, color = colors.ink),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$ratedCount bouts rated on this phone.",
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.mute),
        )
    }
}

@Composable
private fun EmptyState(message: String, onPickAnotherDay: () -> Unit) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.mute, lineHeight = 21.sp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onPickAnotherDay,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
        ) {
            Text("Pick another day", style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 14.sp))
        }
    }
}

private enum class SwipePhase { IDLE, DRAGGING }

@Composable
private fun BoutCard(
    bout: Bout,
    w: Int?,
    l: Int?,
    photos: Map<Int, String>,
    canSwipeRight: Boolean,
    onWinnerSettled: (Int) -> Unit,
    onLoserSettled: (Int) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onPhotoTap: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animOffsetDp = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    var phase by remember { mutableStateOf(SwipePhase.IDLE) }
    var dragOffsetDp by remember { mutableFloatStateOf(0f) }

    val bounceSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)

    fun settleBack(from: Float) {
        scope.launch {
            animOffsetDp.snapTo(from)
            animOffsetDp.animateTo(0f, bounceSpec)
        }
    }

    fun flyOutThenAdvance(from: Float, direction: Int) {
        scope.launch {
            animOffsetDp.snapTo(from)
            launch { alpha.animateTo(0f, tween(220)) }
            animOffsetDp.animateTo(direction * FLY_DISTANCE_DP, tween(220))
            if (direction < 0) onSwipeLeft() else onSwipeRight()
            animOffsetDp.snapTo(-direction * ENTER_OFFSET_DP)
            alpha.snapTo(0f)
            launch { alpha.animateTo(1f, tween(250)) }
            animOffsetDp.animateTo(0f, bounceSpec)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, bottom = 40.dp)
            .pointerInput(canSwipeRight) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        phase = SwipePhase.DRAGGING
                        dragOffsetDp = animOffsetDp.value
                    },
                    onDragEnd = {
                        val d = dragOffsetDp
                        phase = SwipePhase.IDLE
                        if (abs(d) > SWIPE_THRESHOLD_DP && (d < 0 || canSwipeRight)) {
                            flyOutThenAdvance(d, if (d < 0) -1 else 1)
                        } else {
                            settleBack(d)
                        }
                    },
                    onDragCancel = {
                        val d = dragOffsetDp
                        phase = SwipePhase.IDLE
                        settleBack(d)
                    },
                ) { change, dragAmount ->
                    change.consume()
                    dragOffsetDp += dragAmount / density
                }
            },
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val dp = if (phase == SwipePhase.DRAGGING) dragOffsetDp else animOffsetDp.value
                    translationX = dp * density
                    rotationZ = dp / 40f
                    this.alpha = alpha.value
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RikishiColumn(name = bout.winnerName, rank = bout.winnerRank, photoPath = photos[bout.winnerId], onPhotoTap = { onPhotoTap(bout.winnerId) })

            val winnerItems = remember { (10 downTo 1).map { it.toString() } + "" }
            val winnerTarget = if (w == null) 10 else 10 - w
            WheelPicker(
                items = winnerItems,
                targetIndex = winnerTarget,
                onSettledIndexChange = onWinnerSettled,
                modifier = Modifier
                    .align(Alignment.Top)
                    .offset(y = (-30).dp),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Top)
                    .offset(y = (-30).dp)
                    .width(18.dp)
                    .height(3.dp)
                    .background(LocalSumoColors.current.ink),
            )

            val loserMax = w ?: 11
            val loserItems = remember(loserMax) { ((loserMax - 1) downTo 0).map { it.toString() } + "" }
            val loserTarget = if (l == null) loserMax else loserMax - 1 - l
            WheelPicker(
                items = loserItems,
                targetIndex = loserTarget,
                onSettledIndexChange = onLoserSettled,
                modifier = Modifier
                    .align(Alignment.Top)
                    .offset(y = (-30).dp),
            )

            RikishiColumn(name = bout.loserName, rank = bout.loserRank, photoPath = photos[bout.loserId], onPhotoTap = { onPhotoTap(bout.loserId) })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(66.dp, Alignment.CenterHorizontally),
        ) {
            val faintCaption = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.18f.em, color = LocalSumoColors.current.faint)
            Text("WIN", style = faintCaption)
            Text("LOSS", style = faintCaption)
        }
    }
}

@Composable
private fun RikishiColumn(name: String, rank: String, photoPath: String?, onPhotoTap: () -> Unit) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RikishiPhoto(
            photoPath = photoPath,
            onClick = onPhotoTap,
            modifier = Modifier.size(width = 92.dp, height = 118.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 16.sp, color = colors.ink),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = rank,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.04f.em, color = colors.mute),
        )
    }
}
