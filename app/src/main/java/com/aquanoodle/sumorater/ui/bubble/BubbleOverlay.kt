package com.aquanoodle.sumorater.ui.bubble

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.data.Bout
import com.aquanoodle.sumorater.data.bashoLabel
import com.aquanoodle.sumorater.ui.SumoController
import com.aquanoodle.sumorater.ui.components.WheelPicker
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.SumoRaterTheme
import com.aquanoodle.sumorater.ui.theme.TabularNums
import com.aquanoodle.sumorater.ui.wheelIndexOf
import com.aquanoodle.sumorater.ui.wheelItems
import com.aquanoodle.sumorater.ui.wheelTop
import kotlin.math.abs

private val WHEEL_WIDTH = 52.dp
private val WHEEL_ROW = 48.dp
private val DASH_WIDTH = 14.dp
private val GAP = 6.dp

@Composable
fun BubbleOverlay(
    controller: SumoController,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
) {
    SumoRaterTheme(darkTheme = controller.state.isDark) {
        if (expanded) {
            RatingPanel(controller, onCollapse, onClose, onOpenApp, onDragBy)
        } else {
            BubbleIcon(onTap = onExpand, onDragBy = onDragBy)
        }
    }
}

/**
 * The overlay window itself moves while dragging, so positions relative to the
 * window stay put under the finger; track the screen-absolute raw coordinates instead.
 */
private class DragTracker(private val slopPx: Float) {
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    fun onEvent(e: MotionEvent, onDragBy: (Float, Float) -> Unit, onTap: (() -> Unit)?): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX; downY = e.rawY
                lastX = downX; lastY = downY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && (abs(e.rawX - downX) > slopPx || abs(e.rawY - downY) > slopPx)) dragging = true
                if (dragging) onDragBy(e.rawX - lastX, e.rawY - lastY)
                lastX = e.rawX; lastY = e.rawY
            }
            MotionEvent.ACTION_UP -> if (!dragging) onTap?.invoke()
        }
        return true
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Modifier.windowDrag(onDragBy: (Float, Float) -> Unit, onTap: (() -> Unit)? = null): Modifier {
    val slop = with(LocalDensity.current) { 8.dp.toPx() }
    val tracker = remember { DragTracker(slop) }
    return this.pointerInteropFilter { tracker.onEvent(it, onDragBy, onTap) }
}

@Composable
private fun BubbleIcon(onTap: () -> Unit, onDragBy: (Float, Float) -> Unit) {
    val colors = LocalSumoColors.current
    Box(
        modifier = Modifier
            .size(BubbleService.BUBBLE_DP.dp)
            .clip(CircleShape)
            .background(colors.ink)
            .windowDrag(onDragBy = onDragBy, onTap = onTap),
        contentAlignment = Alignment.Center,
    ) {
        // Same dohyo mark as the launcher icon.
        Canvas(modifier = Modifier.size(30.dp)) {
            drawCircle(color = colors.bg, radius = size.minDimension / 2 - 1.5.dp.toPx(), style = Stroke(width = 3.dp.toPx()))
            drawCircle(color = colors.bg, radius = size.minDimension * 0.24f)
        }
    }
}

@Composable
private fun RatingPanel(
    controller: SumoController,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onDragBy: (Float, Float) -> Unit,
) {
    val colors = LocalSumoColors.current
    val state = controller.state
    val bout = state.bouts.getOrNull(state.idx)
    val isDone = state.bouts.isNotEmpty() && bout == null
    val canBack = state.idx > 0 && state.bouts.isNotEmpty()
    val shape = RoundedCornerShape(16.dp)

    val label = if (bout != null) {
        "${bout.div} · Day ${state.day} · ${state.idx + 1} / ${state.bouts.size}"
    } else {
        "${bashoLabel(state.bashoId)} · Day ${state.day}"
    }.uppercase()

    Column(
        modifier = Modifier
            .width(BubbleService.PANEL_WIDTH_DP.dp)
            .clip(shape)
            .background(colors.bg)
            .border(1.dp, colors.line, shape),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            // The label area doubles as the drag handle for moving the panel.
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().windowDrag(onDragBy = onDragBy),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(start = 16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = Archivo, fontSize = 11.sp, letterSpacing = 0.12f.em, color = colors.mute, fontFeatureSettings = TabularNums),
                )
            }
            HeaderButton("—", onCollapse)
            HeaderButton("×", onClose)
        }

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            when {
                bout != null -> BoutRow(controller, bout, state.east, state.west)
                isDone -> PanelMessage("${bashoLabel(state.bashoId)} · Day ${state.day} done")
                state.loading -> PanelMessage("Loading…")
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PanelMessage("Pick a basho and day in the app first.")
                    FooterButton("Open app", bold = true, onClick = onOpenApp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canBack) FooterButton("← Back", bold = false, onClick = controller::back) else Spacer(Modifier.size(48.dp))
            if (bout != null) FooterButton("Next →", bold = true, onClick = controller::save)
        }
    }
}

@Composable
private fun BoutRow(controller: SumoController, bout: Bout, east: Int?, west: Int?) {
    val colors = LocalSumoColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(GAP, Alignment.CenterHorizontally),
        // Names centre on the wheels' middle (selected) row.
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NameColumn(bout.eastName, bout.eastRank)
        val reason = bout.unratableReason
        if (reason == null) {
            val eastTop = wheelTop(bout, east, west, isEast = true)
            val westTop = wheelTop(bout, east, west, isEast = false)
            key(bout.key) {
                WheelPicker(
                    items = remember(eastTop) { wheelItems(eastTop) },
                    targetIndex = wheelIndexOf(eastTop, east),
                    onSettledIndexChange = controller::onEastSettled,
                    width = WHEEL_WIDTH, rowHeight = WHEEL_ROW, fontSize = 34.sp,
                )
            }
            Box(Modifier.width(DASH_WIDTH).height(3.dp).background(colors.ink))
            key(bout.key) {
                WheelPicker(
                    items = remember(westTop) { wheelItems(westTop) },
                    targetIndex = wheelIndexOf(westTop, west),
                    onSettledIndexChange = controller::onWestSettled,
                    width = WHEEL_WIDTH, rowHeight = WHEEL_ROW, fontSize = 34.sp,
                )
            }
        } else {
            Box(
                modifier = Modifier.width(WHEEL_WIDTH * 2 + DASH_WIDTH + GAP * 2).height(WHEEL_ROW * 3),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(reason, style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.mute))
                    Text("NOT RATED", style = TextStyle(fontFamily = Archivo, fontSize = 9.sp, letterSpacing = 0.18f.em, color = colors.faint))
                }
            }
        }
        NameColumn(bout.westName, bout.westRank)
    }
}

@Composable
private fun NameColumn(name: String, rank: String) {
    val colors = LocalSumoColors.current
    Column(modifier = Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            name,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 15.sp, color = colors.ink),
        )
        Text(rank, style = TextStyle(fontFamily = Archivo, fontSize = 10.sp, letterSpacing = 0.04f.em, color = colors.mute))
    }
}

@Composable
private fun PanelMessage(text: String) {
    val colors = LocalSumoColors.current
    Text(
        text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 28.dp),
        textAlign = TextAlign.Center,
        style = TextStyle(fontFamily = Archivo, fontSize = 14.sp, color = colors.mute),
    )
}

@Composable
private fun HeaderButton(glyph: String, onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    Box(modifier = Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(glyph, style = TextStyle(fontFamily = Archivo, fontSize = 18.sp, color = colors.ink))
    }
}

@Composable
private fun FooterButton(text: String, bold: Boolean, onClick: () -> Unit) {
    val colors = LocalSumoColors.current
    Box(
        modifier = Modifier.sizeIn(minHeight = 48.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(
                fontFamily = Archivo,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = if (bold) 16.sp else 14.sp,
                color = if (bold) colors.ink else colors.faint,
            ),
        )
    }
}
