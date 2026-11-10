package com.aquanoodle.sumorater.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
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
import com.aquanoodle.sumorater.ui.wheelIndexOf
import com.aquanoodle.sumorater.ui.wheelItems
import com.aquanoodle.sumorater.ui.wheelTop
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_THRESHOLD_DP = 70f
private const val FLY_DISTANCE_DP = 520f
private const val ENTER_OFFSET_DP = 90f

// Must match WheelPicker's fixed viewport height (its selected row is always
// centered within it) and the photo size below, so the photo, the dash, and
// the wheels' selected numbers all line up on the same horizontal line.
private const val WHEEL_VIEWPORT_DP = 192
private const val WHEEL_WIDTH_DP = 60
private const val DASH_WIDTH_DP = 18
private const val GAP_DP = 8
private const val PHOTO_HEIGHT_DP = 118
private const val PHOTO_TOP_OFFSET_DP = (WHEEL_VIEWPORT_DP - PHOTO_HEIGHT_DP) / 2

@Composable
fun RateScreen(
    bouts: List<Bout>,
    idx: Int,
    day: Int,
    bashoLabel: String,
    loading: Boolean,
    east: Int?,
    west: Int?,
    note: String,
    ratedToday: Int,
    photos: Map<Int, String>,
    onOpenMenu: () -> Unit,
    onOpenTable: () -> Unit,
    onEastSettled: (Int) -> Unit,
    onWestSettled: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRestartDay: () -> Unit,
    onPhotoTap: (Int) -> Unit,
) {
    val colors = LocalSumoColors.current
    val bout = bouts.getOrNull(idx)
    val isDone = bouts.isNotEmpty() && bout == null
    val isEmpty = bouts.isEmpty()
    // Back stays available on the "day done" screen, so a finished day can be reopened.
    val canBack = idx > 0 && bouts.isNotEmpty()

    val progressLabel = if (bout != null) {
        "${bout.div} · Day $day · ${idx + 1} / ${bouts.size}".uppercase()
    } else {
        "$bashoLabel · Day $day".uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
    ) {
        TopBar(progressLabel = progressLabel, onOpenMenu = onOpenMenu)

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.Center,
            ) {
                when {
                    bout != null -> BoutCard(
                        bout = bout,
                        east = east,
                        west = west,
                        note = note,
                        photos = photos,
                        canSwipeRight = canBack,
                        onEastSettled = onEastSettled,
                        onWestSettled = onWestSettled,
                        onNoteChange = onNoteChange,
                        onSwipeLeft = onNext,
                        onSwipeRight = onBack,
                        onPhotoTap = onPhotoTap,
                    )
                    isDone -> DoneState(
                        bashoLabel = bashoLabel,
                        day = day,
                        ratedCount = ratedToday,
                        onOpenTable = onOpenTable,
                        onRestartDay = onRestartDay,
                    )
                    isEmpty -> EmptyState(
                        message = if (loading) "Loading…" else "No bout data available for $bashoLabel, day $day.",
                        onPickAnotherDay = onOpenMenu,
                    )
                }
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
private fun DoneState(
    bashoLabel: String,
    day: Int,
    ratedCount: Int,
    onOpenTable: () -> Unit,
    onRestartDay: () -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onOpenTable,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
        ) {
            Text("View my ratings", style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 14.sp))
        }
        TextButton(onClick = onRestartDay) {
            Text("Back to first bout", style = TextStyle(fontFamily = Archivo, fontSize = 13.sp, color = colors.faint))
        }
    }
}

@Composable
private fun EmptyState(message: String, onPickAnotherDay: () -> Unit) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
    east: Int?,
    west: Int?,
    note: String,
    photos: Map<Int, String>,
    canSwipeRight: Boolean,
    onEastSettled: (Int) -> Unit,
    onWestSettled: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onPhotoTap: (Int) -> Unit,
) {
    val colors = LocalSumoColors.current
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

    // The whole card moves with the swipe; only the wrestler row (not the notes box) starts one.
    val cardTransform = Modifier.graphicsLayer {
        val dp = if (phase == SwipePhase.DRAGGING) dragOffsetDp else animOffsetDp.value
        translationX = dp * density
        rotationZ = dp / 40f
        this.alpha = alpha.value
    }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().then(cardTransform),
                horizontalArrangement = Arrangement.spacedBy(GAP_DP.dp, Alignment.CenterHorizontally),
                // Top-aligned: the wheel's selected row always sits at its own vertical
                // center (WHEEL_VIEWPORT_DP / 2), so every sibling is pinned to line up
                // with that same height explicitly.
                verticalAlignment = Alignment.Top,
            ) {
                RikishiColumn(name = bout.eastName, rank = bout.eastRank, photoPath = photos[bout.eastId], onPhotoTap = { onPhotoTap(bout.eastId) })

                val reason = bout.unratableReason
                if (reason == null) {
                    val eastTop = wheelTop(bout, east, west, isEast = true)
                    val westTop = wheelTop(bout, east, west, isEast = false)
                    // Keyed per bout so a wheel still settling can't write into the next bout.
                    key(bout.key) {
                        WheelPicker(
                            items = remember(eastTop) { wheelItems(eastTop) },
                            targetIndex = wheelIndexOf(eastTop, east),
                            onSettledIndexChange = onEastSettled,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(DASH_WIDTH_DP.dp)
                            .height(WHEEL_VIEWPORT_DP.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(colors.ink),
                        )
                    }
                    key(bout.key) {
                        WheelPicker(
                            items = remember(westTop) { wheelItems(westTop) },
                            targetIndex = wheelIndexOf(westTop, west),
                            onSettledIndexChange = onWestSettled,
                        )
                    }
                } else {
                    UnratableMiddle(reason)
                }

                RikishiColumn(name = bout.westName, rank = bout.westRank, photoPath = photos[bout.westId], onPhotoTap = { onPhotoTap(bout.westId) })
            }
        }

        if (bout.ratable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .then(cardTransform),
                horizontalArrangement = Arrangement.spacedBy(66.dp, Alignment.CenterHorizontally),
            ) {
                val faintCaption = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.18f.em, color = colors.faint)
                Text("EAST", style = faintCaption)
                Text("WEST", style = faintCaption)
            }

            key(bout.key) {
                NotesSection(note = note, onNoteChange = onNoteChange, modifier = Modifier.then(cardTransform))
            }
        }
    }
}

@Composable
private fun UnratableMiddle(reason: String) {
    val colors = LocalSumoColors.current
    Box(
        modifier = Modifier
            .width((WHEEL_WIDTH_DP * 2 + DASH_WIDTH_DP + GAP_DP * 2).dp)
            .height(WHEEL_VIEWPORT_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(reason, textAlign = TextAlign.Center, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.mute))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "NOT RATED",
                style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.18f.em, color = colors.faint),
            )
        }
    }
}

/** Collapsed by default; opens automatically when the bout already has a note. */
@Composable
private fun NotesSection(note: String, onNoteChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSumoColors.current
    var expanded by remember { mutableStateOf(note.isNotBlank()) }
    Column(modifier = modifier.fillMaxWidth().padding(top = 20.dp, start = 12.dp, end = 12.dp)) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .sizeIn(minHeight = 40.dp)
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val label = if (!expanded && note.isNotBlank()) "NOTES ·" else "NOTES"
            Text(label, style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.18f.em, color = colors.faint))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (expanded) "▴" else "▾", style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = colors.faint))
        }
        if (expanded) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                textStyle = TextStyle(fontFamily = Archivo, fontSize = 14.sp, lineHeight = 20.sp, color = colors.ink),
                cursorBrush = SolidColor(colors.ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.line)
                    .padding(12.dp)
                    .heightIn(min = 60.dp),
                decorationBox = { inner ->
                    Box {
                        if (note.isEmpty()) {
                            Text("Add a note…", style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.faint))
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun RikishiColumn(name: String, rank: String, photoPath: String?, onPhotoTap: () -> Unit) {
    val colors = LocalSumoColors.current
    Column(
        modifier = Modifier
            .width(104.dp)
            .padding(top = PHOTO_TOP_OFFSET_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RikishiPhoto(
            photoPath = photoPath,
            onClick = onPhotoTap,
            modifier = Modifier.size(width = 92.dp, height = PHOTO_HEIGHT_DP.dp),
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
