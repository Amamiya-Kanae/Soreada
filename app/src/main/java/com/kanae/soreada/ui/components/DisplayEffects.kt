package com.kanae.soreada.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Color as AndroidColor
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.kanae.soreada.data.AppSettings
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun EyeCareOverlay(settings: AppSettings, modifier: Modifier = Modifier) {
    if (!settings.eyeCareEnabled || settings.eyeCareStrength <= 0f) return
    val alpha = (0.04f + settings.eyeCareStrength * 0.18f).coerceIn(0f, 0.24f)
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFFFFB25C).copy(alpha = alpha)),
    )
}

@Composable
fun PaperGrainOverlay(strength: Float, modifier: Modifier = Modifier) {
    if (strength <= 0f) return
    val tile = remember { createGrainTile() }
    val brush = remember(tile) {
        ShaderBrush(
            BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT),
        )
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = brush,
            alpha = (strength * 0.35f).coerceIn(0f, 0.4f),
            blendMode = BlendMode.SrcOver,
        )
    }
}

private fun createGrainTile(): Bitmap {
    val size = 192
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val random = Random(20261023)
    val pixels = IntArray(size * size)
    for (index in pixels.indices) {
        val lighter = random.nextBoolean()
        val gray = if (lighter) random.nextInt(230, 256) else random.nextInt(0, 45)
        val alpha = random.nextInt(90, 180)
        pixels[index] = AndroidColor.argb(alpha, gray, gray, gray)
    }
    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
    return bitmap
}
