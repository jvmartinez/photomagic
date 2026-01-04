package com.devsapiens.phonemagic.model

import android.graphics.Bitmap
import android.net.Uri

data class FilterParams(
    val brightness: Float = 0f, // -1..1
    val contrast: Float = 1f,   // 0..2
    val saturation: Float = 1f, // 0..2
    val sepia: Float = 0f,      // 0..1 intensity
    // Optional stylized filter id (e.g. "retro", "vhs", "polaroid", "year90", "grain", "film")
    val stylize: String? = null,
    // Intensity for the stylized effect (0..1)
    val stylizeIntensity: Float = 1f
)

// New Filter A params: a simple aesthetic filter with intensity, warmth and vignette
data class FilterAParams(
    val intensity: Float = 0f, // 0..1
    val warmth: Float = 0f,    // -1..1 (negative=cooler, positive=warmer)
    val vignette: Float = 0f   // 0..1
)

// New Filter B params: split-tone style with shadow/highlight tint and strength
data class FilterBParams(
    val highlightsTint: Int = 0xFFFFFF, // RGB hex
    val shadowsTint: Int = 0x000000,
    val strength: Float = 0f // 0..1
)

data class EnhanceParams(
    val sharpen: Float = 0f,    // 0..1
    val denoise: Float = 0f,    // 0..1
    val upscaleRequested: Boolean = false
)

sealed interface Layer {
    data class Sticker(val resId: Int, val x: Float, val y: Float, val scale: Float, val rotation: Float) : Layer
    data class Text(
        val text: String,
        val color: Int,
        val sizeSp: Float,
        val x: Float,
        val y: Float,
        val rotation: Float
    ) : Layer
}

data class EditorState(
    val imageUri: Uri? = null,
    val baseBitmap: Bitmap? = null,
    val filter: FilterParams = FilterParams(),
    val filterA: FilterAParams = FilterAParams(),
    val filterB: FilterBParams = FilterBParams(),
    val enhance: EnhanceParams = EnhanceParams(),
    val layers: List<Layer> = emptyList()
)
