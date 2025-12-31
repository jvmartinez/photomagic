package com.devsapiens.phonemagic.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.devsapiens.phonemagic.model.EnhanceParams
import com.devsapiens.phonemagic.model.FilterAParams
import com.devsapiens.phonemagic.model.FilterBParams
import com.devsapiens.phonemagic.model.FilterParams
import com.devsapiens.phonemagic.model.EditorState

object ImageProcessor {

    // Apply common filter params: brightness (-1..1), contrast (0..2), saturation (0..2), sepia (0..1)
    fun applyFilterParams(src: Bitmap, params: FilterParams): Bitmap {
        val out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val cm = ColorMatrix()
        // Contrast
        val contrast = params.contrast.coerceIn(0f, 2f)
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        // Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(params.saturation.coerceIn(0f, 2f))
        cm.postConcat(satMatrix)

        // Brightness: add offset to RGB
        val brightness = (params.brightness.coerceIn(-1f, 1f) * 255f)
        val brightMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(brightMatrix)

        val paint = Paint()
        paint.isFilterBitmap = true
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(out, 0f, 0f, paint)

        // Sepia overlay: draw a sepia-color-filtered version on top with alpha equal to sepia intensity
        val sepia = params.sepia.coerceIn(0f, 1f)
        if (sepia > 0f) {
            val sepiaMatrix = ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            val sepiaPaint = Paint()
            sepiaPaint.isFilterBitmap = true
            sepiaPaint.colorFilter = ColorMatrixColorFilter(sepiaMatrix)
            sepiaPaint.alpha = (sepia * 255f).toInt().coerceIn(0, 255)
            // Draw the sepia version on top of current canvas
            canvas.drawBitmap(out, 0f, 0f, sepiaPaint)
        }

        return out
    }

    // Apply Filter A (warmth/intensity/vignette) to a bitmap
    fun applyFilterAParams(src: Bitmap, params: FilterAParams): Bitmap {
        val out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        // Warmth & intensity via channel scaling + saturation
        val redScale = 1f + (params.warmth * 0.25f)
        val blueScale = 1f - (params.warmth * 0.25f)
        val i = params.intensity.coerceIn(0f, 1f)
        val cm = ColorMatrix()
        cm.setScale(
            1f + (redScale - 1f) * i,
            1f + (1f - 1f) * i,
            1f + (blueScale - 1f) * i,
            1f
        )
        val sat = 1f + 0.35f * i
        val satMat = ColorMatrix()
        satMat.setSaturation(sat)
        cm.postConcat(satMat)

        val paint = Paint()
        paint.isFilterBitmap = true
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(out, 0f, 0f, paint)

        // Vignette overlay
        if (params.vignette > 0f) {
            val overlayPaint = Paint()
            val cx = out.width / 2f
            val cy = out.height / 2f
            val radius = kotlin.math.max(out.width, out.height) * 0.7f
            val vignetteAlpha = (params.vignette.coerceIn(0f, 1f) * 0.75f)
            val vignetteColor = Color.argb((vignetteAlpha * 255f).toInt().coerceIn(0, 255), 0, 0, 0)
            val shader = RadialGradient(cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, vignetteColor),
                floatArrayOf(0.5f, 1f), Shader.TileMode.CLAMP)
            overlayPaint.shader = shader
            canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), overlayPaint)
        }

        return out
    }

    // Apply Filter B: split-tone shadows/highlights using per-pixel luminance blending
    fun applyFilterBParams(src: Bitmap, params: FilterBParams): Bitmap {
        val out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val width = out.width
        val height = out.height
        val strength = params.strength.coerceIn(0f, 1f)
        // Extract tints (ints are RGB hex values)
        val hi = params.highlightsTint and 0xFFFFFF
        val sh = params.shadowsTint and 0xFFFFFF
        val hiR = ((hi shr 16) and 0xFF) / 255f
        val hiG = ((hi shr 8) and 0xFF) / 255f
        val hiB = (hi and 0xFF) / 255f
        val shR = ((sh shr 16) and 0xFF) / 255f
        val shG = ((sh shr 8) and 0xFF) / 255f
        val shB = (sh and 0xFF) / 255f

        val pixels = IntArray(width * height)
        out.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            // luminance (0..1)
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            // shadow factor: 1 - lum, highlight factor: lum
            val shFactor = (1f - lum)
            val hiFactor = lum
            // apply tints
            val newR = (r / 255f * (1f - strength) + (shR * shFactor + hiR * hiFactor) * strength) * 255f
            val newG = (g / 255f * (1f - strength) + (shG * shFactor + hiG * hiFactor) * strength) * 255f
            val newB = (b / 255f * (1f - strength) + (shB * shFactor + hiB * hiFactor) * strength) * 255f
            pixels[i] = Color.argb(a, newR.coerceIn(0f, 255f).toInt(), newG.coerceIn(0f, 255f).toInt(), newB.coerceIn(0f, 255f).toInt())
        }
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    // Placeholder for enhance (sharpen/denoise) - currently no-op
    @Suppress("UNUSED_PARAMETER")
    fun applyEnhanceParams(src: Bitmap, params: EnhanceParams): Bitmap {
        // Implement real sharpen/denoise later. Return src for now.
        return src
    }

    // Compose all processing steps using the EditorState
    fun processAll(state: EditorState): Bitmap? {
        val base = state.baseBitmap ?: return null
        var bmp = base
        // Apply basic filter params
        bmp = applyFilterParams(bmp, state.filter)
        // Apply Filter A
        bmp = applyFilterAParams(bmp, state.filterA)
        // Apply Filter B
        bmp = applyFilterBParams(bmp, state.filterB)
        // Apply enhance (no-op currently)
        bmp = applyEnhanceParams(bmp, state.enhance)

        // Rasterize layers (stickers and text) on top of the processed bitmap
        if (state.layers.isNotEmpty()) {
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (layer in state.layers) {
                when (layer) {
                    is com.devsapiens.phonemagic.model.Layer.Sticker -> {
                        // Draw a simple circular placeholder for sticker
                        paint.style = Paint.Style.FILL
                        paint.color = Color.YELLOW
                        val radius = 24f * (layer.scale.coerceAtLeast(0.1f))
                        canvas.drawCircle(layer.x, layer.y, radius, paint)
                    }

                    is com.devsapiens.phonemagic.model.Layer.Text -> {
                        // Draw text layer
                        paint.style = Paint.Style.FILL
                        paint.color = layer.color
                        // Use sizeSp as pixel size approximation
                        paint.textSize = layer.sizeSp.coerceAtLeast(12f)
                        canvas.save()
                        canvas.translate(layer.x, layer.y)
                        canvas.rotate(layer.rotation)
                        canvas.drawText(layer.text, 0f, 0f + paint.textSize, paint)
                        canvas.restore()
                    }
                }
            }
        }

        return bmp
    }
}
