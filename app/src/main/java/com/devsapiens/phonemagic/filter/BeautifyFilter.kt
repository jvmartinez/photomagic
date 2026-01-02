package com.devsapiens.phonemagic.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Filtro de embellecimiento (beautify) sencillo y compatible:
 * - Downscale + upscale para simular un blur rápido
 * - Mezcla del blur con la original según `intensity` (0..1)
 * - Ajustes de saturación y calidez (warmth)
 *
 * Uso: BeautifyFilter.applyBeautify(bitmap, intensity = 0.6f, saturation = 0.95f, warmth = 0.06f)
 */
object BeautifyFilter {
    /**
     * Aplica embellecimiento sobre un bitmap de entrada y devuelve un nuevo bitmap.
     * No modifica el bitmap original.
     * @param src Bitmap de entrada
     * @param intensity 0..1 mezcla del blur con la imagen original
     * @param saturation multiplicador de saturación (1 = sin cambios)
     * @param warmth valor de calidez - rangos típicos 0..0.2 (positivo = más cálido)
     */
    fun applyBeautify(
        src: Bitmap,
        intensity: Float = 0.6f,
        saturation: Float = 0.95f,
        warmth: Float = 0.06f
    ): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val sat = saturation.coerceAtLeast(0f)
        val warm = warmth

        // Fast blur via downscale-upscale. Downscale factor depends on intensity: más intensidad -> más blur
        val minFactor = 0.35f // nivel máximo de downscale (más blur)
        val maxFactor = 1.0f  // sin blur
        val downscaleFactor = (maxFactor - (maxFactor - minFactor) * t).coerceIn(minFactor, maxFactor)

        val smallW = max(1, (src.width * downscaleFactor).roundToInt())
        val smallH = max(1, (src.height * downscaleFactor).roundToInt())

        // Crear bitmap reducido y luego reescalar para simular blur
        val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val blurred = Bitmap.createScaledBitmap(small, src.width, src.height, true)
        if (small !== blurred && !small.isRecycled) {
            small.recycle()
        }

        // Mezclar blurred sobre original usando alpha según intensidad
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (t * 255).roundToInt()
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        if (!blurred.isRecycled) blurred.recycle()

        // Aplicar ajustes de color: saturación y leve calentamiento
        val cm = ColorMatrix()
        cm.setSaturation(sat)

        // Compose a small warmth shift: add to R channel, subtract a fraction from B channel
        val warmthOffset = (warm * 255f).coerceIn(-64f, 64f)
        val translate = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, warmthOffset,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, -warmthOffset * 0.5f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(translate)

        val result = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
        val c2 = Canvas(result)
        val paint2 = Paint(Paint.FILTER_BITMAP_FLAG)
        paint2.colorFilter = ColorMatrixColorFilter(cm)
        c2.drawBitmap(out, 0f, 0f, paint2)
        if (!out.isRecycled) out.recycle()

        return result
    }
}

