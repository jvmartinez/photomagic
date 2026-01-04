package com.devsapiens.phonemagic.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min
import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import java.util.Random

object StylizedFilters {
    private fun ensureArgb(src: Bitmap): Bitmap {
        // Always return a mutable ARGB_8888 bitmap copy to work safely
        return if (src.config == Bitmap.Config.ARGB_8888) src.copy(Bitmap.Config.ARGB_8888, true) else src.copy(Bitmap.Config.ARGB_8888, true)
    }

    // Retro: slight sepia + reduced contrast + subtle vignetting
    fun retro(src: Bitmap, intensity: Float = 0.8f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val out = ensureArgb(src)
        val canvas = Canvas(out)

        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val a = Color.alpha(c)
            var rf = Color.red(c).toFloat()
            var gf = Color.green(c).toFloat()
            var bf = Color.blue(c).toFloat()

            // mild desaturation
            val avg = (rf + gf + bf) / 3f
            rf = (rf + avg) * 0.5f
            gf = (gf + avg) * 0.5f
            bf = (bf + avg) * 0.5f

            // warm boost
            rf += 20f * t
            bf -= 10f * t

            // soft contrast
            val factor = 1f + 0.12f * t
            rf = ((rf - 128f) * factor + 128f)
            gf = ((gf - 128f) * factor + 128f)
            bf = ((bf - 128f) * factor + 128f)

            val r = rf.roundToInt().coerceIn(0, 255)
            val g = gf.roundToInt().coerceIn(0, 255)
            val b = bf.roundToInt().coerceIn(0, 255)

            pixels[i] = Color.argb(a, r, g, b)
            i++
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)

        // slight vignette overlay (very subtle)
        if (t > 0f) {
            val overlay = Paint().apply { color = Color.argb((80f * t).toInt().coerceIn(0, 200), 0, 0, 0) }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlay)
        }

        return out
    }

    // VHS: RGB split + scanlines + subtle noise
    fun vhs(src: Bitmap, intensity: Float = 0.8f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val out = ensureArgb(src)
        val w = out.width
        val h = out.height

        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)
        val shift = (4f * t).roundToInt()
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]
                val a = Color.alpha(c)
                val g = Color.green(c)

                val rx = (x - shift).coerceIn(0, w - 1)
                val bx = (x + shift).coerceIn(0, w - 1)
                val rC = pixels[y * w + rx]
                val bC = pixels[y * w + bx]

                val nr = (Color.red(rC) * (0.9f + 0.1f * t)).coerceIn(0f, 255f).roundToInt()
                val ng = (g * (0.95f + 0.05f * t)).roundToInt().coerceIn(0, 255)
                val nb = (Color.blue(bC) * (0.9f + 0.1f * t)).coerceIn(0f, 255f).roundToInt()

                outPixels[idx] = Color.argb(a, nr, ng, nb)
            }
        }

        val bitmap = Bitmap.createBitmap(outPixels, w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.argb((50f * t).toInt(), 0, 0, 0)
        for (y in 0 until h step 2) {
            canvas.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), paint)
        }

        // Add subtle noise/grain
        val rnd = Random(0)
        val noise = IntArray(w * h)
        bitmap.getPixels(noise, 0, w, 0, 0, w, h)
        for (i in noise.indices) {
            val c = noise[i]
            val a = Color.alpha(c)
            var rf = Color.red(c)
            var gf = Color.green(c)
            var bf = Color.blue(c)
            val n = ((rnd.nextFloat() - 0.5f) * 30f * t).roundToInt()
            rf = (rf + n).coerceIn(0, 255)
            gf = (gf + n).coerceIn(0, 255)
            bf = (bf + n).coerceIn(0, 255)
            noise[i] = Color.argb(a, rf, gf, bf)
        }
        bitmap.setPixels(noise, 0, w, 0, 0, w, h)
        return bitmap
    }

    // Polaroid: boost saturation, slight vignette, soft fade
    fun polaroid(src: Bitmap, intensity: Float = 0.9f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val out = ensureArgb(src)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val a = Color.alpha(c)
            var rf = Color.red(c).toFloat()
            var gf = Color.green(c).toFloat()
            var bf = Color.blue(c).toFloat()

            val avg = (rf + gf + bf) / 3f
            rf = (rf + ((rf - avg) * (0.4f * t))).coerceIn(0f, 255f)
            gf = (gf + ((gf - avg) * (0.4f * t))).coerceIn(0f, 255f)
            bf = (bf + ((bf - avg) * (0.4f * t))).coerceIn(0f, 255f)

            val factor = 1f + 0.08f * t
            rf = ((rf - 128f) * factor + 128f)
            gf = ((gf - 128f) * factor + 128f)
            bf = ((bf - 128f) * factor + 128f)

            val r = rf.roundToInt().coerceIn(0, 255)
            val g = gf.roundToInt().coerceIn(0, 255)
            val b = bf.roundToInt().coerceIn(0, 255)

            pixels[i] = Color.argb(a, r, g, b)
            i++
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(out)
        val overlay = Paint()
        overlay.color = Color.argb((40f * t).toInt().coerceIn(0, 200), 255, 244, 214)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlay)

        if (t > 0f) {
            val v = Paint().apply { color = Color.argb((60f * t).toInt().coerceIn(0, 200), 0, 0, 0) }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), v)
        }

        return out
    }

    // Year90: high contrast, heavy saturation, slight posterize
    fun year90(src: Bitmap, intensity: Float = 0.9f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val out = ensureArgb(src)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val a = Color.alpha(c)
            var rf = Color.red(c).toFloat()
            var gf = Color.green(c).toFloat()
            var bf = Color.blue(c).toFloat()

            val avg = (rf + gf + bf) / 3f
            rf = (rf + ((rf - avg) * (0.8f * t))).coerceIn(0f, 255f)
            gf = (gf + ((gf - avg) * (0.8f * t))).coerceIn(0f, 255f)
            bf = (bf + ((bf - avg) * (0.8f * t))).coerceIn(0f, 255f)

            val factor = 1f + 0.3f * t
            rf = ((rf - 128f) * factor + 128f)
            gf = ((gf - 128f) * factor + 128f)
            bf = ((bf - 128f) * factor + 128f)

            val levels = (6 - (4 * (1f - t))).roundToInt().coerceIn(2, 6)
            val step = 256 / levels
            val r = ((rf.roundToInt() / step) * step).coerceIn(0, 255)
            val g = ((gf.roundToInt() / step) * step).coerceIn(0, 255)
            val b = ((bf.roundToInt() / step) * step).coerceIn(0, 255)

            pixels[i] = Color.argb(a, r, g, b)
            i++
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    // Grain / Granulado
    fun grain(src: Bitmap, intensity: Float = 0.5f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val out = ensureArgb(src)
        val w = out.width
        val h = out.height
        val rnd = Random(0)
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = Color.alpha(c)
            var r = Color.red(c)
            var g = Color.green(c)
            var b = Color.blue(c)
            val n = ((rnd.nextFloat() - 0.5f) * 80f * t).roundToInt()
            r = (r + n).coerceIn(0, 255)
            g = (g + n).coerceIn(0, 255)
            b = (b + n).coerceIn(0, 255)
            pixels[i] = Color.argb(a, r, g, b)
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    // Film: emulates film curve with a slight teal shadow and warm highlights + grain
    fun film(src: Bitmap, intensity: Float = 0.8f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        val out = ensureArgb(src)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val a = Color.alpha(c)
            var rf = Color.red(c).toFloat()
            var gf = Color.green(c).toFloat()
            var bf = Color.blue(c).toFloat()

            val lum = (0.299f * rf + 0.587f * gf + 0.114f * bf) / 255f
            val shadowFactor = (1f - lum)
            rf -= (20f * shadowFactor * t)
            gf += (6f * t)
            bf += (12f * t)

            val factor = 1f + 0.12f * t
            rf = ((rf - 128f) * factor + 128f)
            gf = ((gf - 128f) * factor + 128f)
            bf = ((bf - 128f) * factor + 128f)

            val r = rf.roundToInt().coerceIn(0, 255)
            val g = gf.roundToInt().coerceIn(0, 255)
            val b = bf.roundToInt().coerceIn(0, 255)

            pixels[i] = Color.argb(a, r, g, b)
            i++
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)

        // Add film grain
        val rnd = Random(1)
        val grainPixels = IntArray(w * h)
        out.getPixels(grainPixels, 0, w, 0, 0, w, h)
        for (j in grainPixels.indices) {
            val c = grainPixels[j]
            val a = Color.alpha(c)
            var r = Color.red(c)
            var g = Color.green(c)
            var b = Color.blue(c)
            val n = ((rnd.nextFloat() - 0.5f) * 40f * t).roundToInt()
            r = (r + n).coerceIn(0, 255)
            g = (g + n).coerceIn(0, 255)
            b = (b + n).coerceIn(0, 255)
            grainPixels[j] = Color.argb(a, r, g, b)
        }
        out.setPixels(grainPixels, 0, w, 0, 0, w, h)

        return out
    }

    // Fast blur via downscale + upscale. intensity 0..1 (0 = none, 1 = strong)
    fun blur(src: Bitmap, intensity: Float = 0.6f): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        if (t <= 0f) return ensureArgb(src)

        val w = src.width
        val h = src.height

        // Determine downscale factor: more intensity -> smaller temporary bitmap
        val minFactor = 0.25f
        val maxFactor = 1f
        val downscaleFactor = (maxFactor - (maxFactor - minFactor) * t).coerceIn(minFactor, maxFactor)

        val sw = max(1, (w * downscaleFactor).roundToInt())
        val sh = max(1, (h * downscaleFactor).roundToInt())

        // Create downscaled bitmap (filter=true for smoothing) and upscale back
        val tmp = Bitmap.createScaledBitmap(src, sw, sh, true)
        val blurred = Bitmap.createScaledBitmap(tmp, w, h, true)
        if (tmp != blurred && !tmp.isRecycled) tmp.recycle()

        // Optionally soften by blending original + blurred according to intensity
        val out = ensureArgb(src)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        // Draw blurred on top with alpha = t
        paint.alpha = (t * 255f).roundToInt().coerceIn(0, 255)
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        if (!blurred.isRecycled) blurred.recycle()

        return out
    }

    // High-quality blur using separable box blur (multiple passes approximate Gaussian)
    fun blurHighQuality(src: Bitmap, intensity: Float = 0.8f, iterations: Int = 3): Bitmap {
        val t = intensity.coerceIn(0f, 1f)
        if (t <= 0f) return ensureArgb(src)

        // Prefer RenderEffect on supported devices (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val w = src.width
                val h = src.height
                val maxDim = max(w, h)
                // map intensity to a blur radius in px (reasonable default)
                val radius = (t * (maxDim * 0.03f)).coerceIn(0.5f, maxDim.toFloat() / 2f)
                val re = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                val out = ensureArgb(src)
                val canvas = Canvas(out)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                // Use reflection to call setRenderEffect to keep compile-time compatibility
                try {
                    val method = Paint::class.java.getMethod("setRenderEffect", RenderEffect::class.java)
                    method.invoke(paint, re)
                    canvas.drawBitmap(src, 0f, 0f, paint)
                    return out
                } catch (_: Throwable) {
                    // Reflection failed (method not available). Fall through to CPU fallback below.
                }
            } catch (_: Throwable) {
                // fall through to CPU fallback on any unexpected error
            }
        }

        // Fallback: separable box blur (previous implementation)
        val out = ensureArgb(src)
        val w = out.width
        val h = out.height
        val maxDim = max(w, h)
        val radiusPx = (t * (maxDim * 0.03f)).roundToInt().coerceIn(1, max(1, (maxDim / 4)))
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        var buffer = IntArray(w * h)

        repeat(iterations) {
            // horizontal
            for (y in 0 until h) {
                var sumA = 0
                var sumR = 0
                var sumG = 0
                var sumB = 0
                val rowStart = y * w
                var x = 0
                while (x <= radiusPx && x < w) {
                    val c = pixels[rowStart + x]
                    sumA += Color.alpha(c)
                    sumR += Color.red(c)
                    sumG += Color.green(c)
                    sumB += Color.blue(c)
                    x++
                }
                for (i in 0 until w) {
                    val addPos = i + radiusPx
                    if (addPos < w) {
                        val c = pixels[rowStart + addPos]
                        sumA += Color.alpha(c)
                        sumR += Color.red(c)
                        sumG += Color.green(c)
                        sumB += Color.blue(c)
                    }
                    val subPos = i - radiusPx - 1
                    if (subPos >= 0) {
                        val c = pixels[rowStart + subPos]
                        sumA -= Color.alpha(c)
                        sumR -= Color.red(c)
                        sumG -= Color.green(c)
                        sumB -= Color.blue(c)
                    }
                    val win = (min(w - 1, i + radiusPx) - max(0, i - radiusPx) + 1)
                    val a = (sumA / win).coerceIn(0, 255)
                    val r = (sumR / win).coerceIn(0, 255)
                    val g = (sumG / win).coerceIn(0, 255)
                    val b = (sumB / win).coerceIn(0, 255)
                    buffer[rowStart + i] = Color.argb(a, r, g, b)
                }
            }
            // vertical
            for (x in 0 until w) {
                var sumA = 0
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var y = 0
                while (y <= radiusPx && y < h) {
                    val c = buffer[y * w + x]
                    sumA += Color.alpha(c)
                    sumR += Color.red(c)
                    sumG += Color.green(c)
                    sumB += Color.blue(c)
                    y++
                }
                for (j in 0 until h) {
                    val addPos = j + radiusPx
                    if (addPos < h) {
                        val c = buffer[addPos * w + x]
                        sumA += Color.alpha(c)
                        sumR += Color.red(c)
                        sumG += Color.green(c)
                        sumB += Color.blue(c)
                    }
                    val subPos = j - radiusPx - 1
                    if (subPos >= 0) {
                        val c = buffer[subPos * w + x]
                        sumA -= Color.alpha(c)
                        sumR -= Color.red(c)
                        sumG -= Color.green(c)
                        sumB -= Color.blue(c)
                    }
                    val win = (min(h - 1, j + radiusPx) - max(0, j - radiusPx) + 1)
                    val a = (sumA / win).coerceIn(0, 255)
                    val r = (sumR / win).coerceIn(0, 255)
                    val g = (sumG / win).coerceIn(0, 255)
                    val b = (sumB / win).coerceIn(0, 255)
                    pixels[j * w + x] = Color.argb(a, r, g, b)
                }
            }
            buffer = IntArray(w * h)
            System.arraycopy(pixels, 0, buffer, 0, pixels.size)
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
