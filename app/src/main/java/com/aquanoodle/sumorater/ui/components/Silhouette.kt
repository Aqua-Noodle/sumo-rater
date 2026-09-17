package com.aquanoodle.sumorater.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aquanoodle.sumorater.ui.theme.LocalSumoColors
import java.io.File

/** Generated silhouette placeholder: rect + circle head + ellipse shoulders, 92x118 proportions. */
@Composable
fun SilhouettePlaceholder(modifier: Modifier = Modifier, cornerRadius: Dp = 6.dp) {
    val colors = LocalSumoColors.current
    Canvas(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        drawRect(color = colors.silhouetteBg)
        val w = size.width
        val h = size.height
        drawCircle(
            color = colors.silhouetteFg,
            radius = w * (20f / 92f),
            center = Offset(w * 0.5f, h * (44f / 118f)),
        )
        val rx = w * (36f / 92f)
        val ry = h * (34f / 118f)
        val cy = h * (112f / 118f)
        drawOval(
            color = colors.silhouetteFg,
            topLeft = Offset(w * 0.5f - rx, cy - ry),
            size = Size(rx * 2, ry * 2),
        )
    }
}

/** Shows the rikishi's custom photo if one was set, else the generated silhouette. Tap to change. */
@Composable
fun RikishiPhoto(
    photoPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
) {
    val clickable = modifier.clip(RoundedCornerShape(cornerRadius)).clickable(onClick = onClick)
    if (photoPath != null) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = clickable,
        )
    } else {
        SilhouettePlaceholder(modifier = clickable, cornerRadius = cornerRadius)
    }
}
