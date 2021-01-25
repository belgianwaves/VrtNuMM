package com.bw.vrtnumm.androidApp.utils

import androidx.compose.animation.animate
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.chrisbanes.accompanist.coil.CoilImage

@Composable
fun VideoThumbnail(
    url: String,
    selected: Boolean = false
): Double {
    val height = 80
    val width = 16.0 * height / 9

    CoilImage(
        data = url.sanitizedUrl(),
        contentScale = ContentScale.Crop,
        modifier = Modifier.preferredHeight(height.dp)
            .preferredWidth(width.dp)
            .clip(MaterialTheme.shapes.medium)
            .graphicsLayer(
                scaleX = animate(if (selected) 1.2f else 1f),
                scaleY = animate(if (selected) 1.2f else 1f)
            )
    )
    return width
}