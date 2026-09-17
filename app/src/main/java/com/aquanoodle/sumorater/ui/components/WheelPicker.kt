package com.aquanoodle.sumorater.ui.components

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquanoodle.sumorater.ui.theme.Archivo
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import com.aquanoodle.sumorater.ui.theme.TabularNums
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val ROW_HEIGHT_DP = 64
private const val VIEWPORT_HEIGHT_DP = 192

/**
 * A swipe-wheel number picker: 60dp wide, 192dp viewport, 64dp rows, fading
 * top/bottom edges, blank row last, snap-to-center. [items] holds the label
 * for every row ("" for the blank row). [targetIndex] is the authoritative
 * index (0-based) to jump to whenever the bout/value changes externally;
 * [onSettledIndexChange] fires ~90ms after the user stops scrolling.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    targetIndex: Int,
    onSettledIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSumoColors.current
    val listState = rememberLazyListState()
    var lastKnownIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(targetIndex, items) {
        if (targetIndex in items.indices && targetIndex != lastKnownIndex) {
            lastKnownIndex = targetIndex
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) {
                delay(90)
                if (!listState.isScrollInProgress) {
                    centeredIndex(listState)?.let { idx ->
                        if (idx != lastKnownIndex) {
                            lastKnownIndex = idx
                            onSettledIndexChange(idx)
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .width(60.dp)
            .height(VIEWPORT_HEIGHT_DP.dp)
            .verticalFadeEdges(),
        flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
        contentPadding = PaddingValues(vertical = ROW_HEIGHT_DP.dp),
    ) {
        itemsIndexed(items) { _, label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT_DP.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = Archivo,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 46.sp,
                        letterSpacing = (-0.03f).em,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        fontFeatureSettings = TabularNums,
                    ),
                )
            }
        }
    }
}

private fun centeredIndex(listState: LazyListState): Int? {
    val info = listState.layoutInfo
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index
}

/** Mirrors the prototype's mask-image: transparent -> opaque 33%-67% -> transparent. */
private fun Modifier.verticalFadeEdges(): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val gradient = Brush.verticalGradient(
                0f to Color.Transparent,
                0.33f to Color.Black,
                0.67f to Color.Black,
                1f to Color.Transparent,
            )
            drawRect(brush = gradient, blendMode = BlendMode.DstIn)
        }
