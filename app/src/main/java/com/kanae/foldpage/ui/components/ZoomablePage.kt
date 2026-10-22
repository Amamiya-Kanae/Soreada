package com.kanae.foldpage.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max

/**
 * Pinch-to-zoom and pan for one comic page.
 *
 * At 1x this modifier deliberately does not consume single-finger drags, so the
 * surrounding pager can still swipe. Once zoomed, it consumes drags to pan and
 * the caller disables pager scrolling.
 */
@Composable
fun ZoomablePage(
    resetKey: Any?,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var scale by remember(resetKey) { mutableFloatStateOf(1f) }
    var offset by remember(resetKey) { mutableStateOf(Offset.Zero) }
    var zoomed by remember(resetKey) { mutableStateOf(false) }
    val latestOnZoomChanged by rememberUpdatedState(onZoomChanged)

    LaunchedEffect(zoomed) {
        latestOnZoomChanged(zoomed)
    }

    Box(
        modifier
            .clipToBounds()
            .pointerInput(resetKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break

                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                val oldScale = scale
                                val nextScale = (scale * zoom).coerceIn(1f, 4f)
                                if (nextScale != oldScale) {
                                    val factor = nextScale / oldScale
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    offset += (centroid - center) * (1f - factor)
                                }
                                offset += pan
                                scale = nextScale
                                if (scale <= 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    offset = clampOffset(offset, scale, size.width.toFloat(), size.height.toFloat())
                                }
                                event.changes.forEach { it.consume() }
                                val nowZoomed = scale > 1.01f
                                if (nowZoomed != zoomed) zoomed = nowZoomed
                            }
                        } else if (scale > 1.01f) {
                            val change = event.changes.firstOrNull { it.pressed } ?: break
                            offset = clampOffset(
                                offset + (change.position - change.previousPosition),
                                scale,
                                size.width.toFloat(),
                                size.height.toFloat(),
                            )
                            change.consume()
                        }
                    }
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            content = content,
        )
    }
}

private fun clampOffset(offset: Offset, scale: Float, width: Float, height: Float): Offset {
    val maxX = max(0f, (scale - 1f) * width / 2f)
    val maxY = max(0f, (scale - 1f) * height / 2f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}
