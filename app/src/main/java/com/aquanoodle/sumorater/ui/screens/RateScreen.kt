package com.aquanoodle.sumorater.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.QUICK_TAGS
import com.aquanoodle.sumorater.data.kimariteLabel
import com.aquanoodle.sumorater.ui.components.RikishiPhoto
import com.aquanoodle.sumorater.ui.components.WheelPicker
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums
import com.aquanoodle.sumorater.ui.wheelIndexOf
import com.aquanoodle.sumorater.ui.wheelItems
import com.aquanoodle.sumorater.ui.wheelTop
import kotlinx.coroutines.CoroutineScope
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateScreen(
    bouts: List<Bout>,
    idx: Int,
    day: Int,
    bashoLabel: String,
    loading: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    east: Int?,
    west: Int?,
    note: String,
    tags: Set<String>,
    ratedToday: Int,
    photos: Map<Int, String>,
    onOpenMenu: () -> Unit,
    onOpenTable: () -> Unit,
    onEastSettled: (Int) -> Unit,
    onWestSettled: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRestartDay: () -> Unit,
    onPhotoTap: (Int) -> Unit,
    onOpenBubble: () -> Unit,
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
        TopBar(progressLabel = progressLabel, onOpenMenu = onOpenMenu, onOpenBubble = onOpenBubble)

        val scope = rememberCoroutineScope()
        val swipe = remember { CardSwipe(scope) }
        val cardTransform = Modifier.graphicsLayer {
            val dp = swipe.offsetDp
            translationX = dp * density
            rotationZ = dp / 40f
            alpha = swipe.alpha
        }
        // The swipe starts anywhere in the middle area; the vertical wheels and the
        // pull-to-refresh still get vertical drags.
        val swipeArea = if (bout == null) Modifier else Modifier.pointerInput(canBack) {
            detectHorizontalDragGestures(
                onDragStart = { swipe.start() },
                onDragEnd = { swipe.end(canBack, onNext, onBack) },
                onDragCancel = { swipe.cancel() },
            ) { change, dragAmount ->
                change.consume()
                swipe.drag(dragAmount / density)
            }
        }
        val pullState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.weight(1f),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = colors.bg,
                    color = colors.ink,
                )
            },
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().then(swipeArea)) {
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
                        tags = tags,
                        photos = photos,
                        cardTransform = cardTransform,
                        onToggleTag = onToggleTag,
                        onEastSettled = onEastSettled,
                        onWestSettled = onWestSettled,
                        onNoteChange = onNoteChange,
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
                        message = if (loading) "Loading…" else "No bout data available for $bashoLabel, day $day. Pull down to try again.",
                        onPickAnotherDay = onOpenMenu,
                    )
                }
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
private fun TopBar(progressLabel: String, onOpenMenu: () -> Unit, onOpenBubble: () -> Unit) {
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
        BubbleButton(onClick = onOpenBubble)
    }
}

/** Opens the floating bubble so bouts can be rated on top of a video app. */
@Composable
private fun BubbleButton(onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Canvas(modifier = Modifier.size(22.dp)) {
            drawCircle(color = colors.ink, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = colors.ink, radius = size.minDimension * 0.2f)
        }
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

/**
 * Swipe-to-change-bout state. Lives at screen level (not inside the card) so the
 * gesture can start anywhere in the middle of the screen and the fly-in animation
 * survives the bout changing underneath it.
 */
@Stable
private class CardSwipe(private val scope: CoroutineScope) {
    private val anim = Animatable(0f)
    private val fade = Animatable(1f)
    private var dragging by mutableStateOf(false)
    private var dragDp by mutableFloatStateOf(0f)
    private val bounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f)

    val offsetDp: Float get() = if (dragging) dragDp else anim.value
    val alpha: Float get() = fade.value

    fun start() {
        dragging = true
        dragDp = anim.value
    }

    fun drag(deltaDp: Float) {
        dragDp += deltaDp
    }

    fun cancel() = settle(stopDragging())

    fun end(canSwipeRight: Boolean, onLeft: () -> Unit, onRight: () -> Unit) {
        val d = stopDragging()
        if (abs(d) > SWIPE_THRESHOLD_DP && (d < 0 || canSwipeRight)) fly(d, if (d < 0) -1 else 1, onLeft, onRight) else settle(d)
    }

    private fun stopDragging(): Float {
        dragging = false
        return dragDp
    }

    private fun settle(from: Float) {
        scope.launch {
            anim.snapTo(from)
            anim.animateTo(0f, bounce)
        }
    }

    private fun fly(from: Float, direction: Int, onLeft: () -> Unit, onRight: () -> Unit) {
        scope.launch {
            anim.snapTo(from)
            launch { fade.animateTo(0f, tween(220)) }
            anim.animateTo(direction * FLY_DISTANCE_DP, tween(220))
            if (direction < 0) onLeft() else onRight()
            anim.snapTo(-direction * ENTER_OFFSET_DP)
            fade.snapTo(0f)
            launch { fade.animateTo(1f, tween(250)) }
            anim.animateTo(0f, bounce)
        }
    }
}

@Composable
private fun BoutCard(
    bout: Bout,
    east: Int?,
    west: Int?,
    note: String,
    tags: Set<String>,
    photos: Map<Int, String>,
    cardTransform: Modifier,
    onEastSettled: (Int) -> Unit,
    onWestSettled: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onPhotoTap: (Int) -> Unit,
) {
    val colors = LocalSumoColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
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

            // Kimarite appears once you've started scoring: same "reveal on interaction"
            // rule as the wheels, so an untouched bout still gives nothing away.
            val kimarite = kimariteLabel(bout.kimarite)
            if (kimarite != null && (east != null || west != null)) {
                Text(
                    kimarite.uppercase(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).then(cardTransform),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.16f.em, color = colors.mute),
                )
            }

            TagChips(selected = tags, onToggle = onToggleTag, modifier = Modifier.then(cardTransform))

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

/** One-tap labels for the bout; quicker than typing a note during a live day. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(selected: Set<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalSumoColors.current
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        QUICK_TAGS.forEach { tag ->
            val on = tag in selected
            Box(
                modifier = Modifier
                    .border(1.dp, if (on) colors.ink else colors.line)
                    .background(if (on) colors.ink else colors.bg)
                    .clickable { onToggle(tag) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(tag, style = TextStyle(fontFamily = Archivo, fontSize = 12.sp, color = if (on) colors.bg else colors.ink))
            }
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
