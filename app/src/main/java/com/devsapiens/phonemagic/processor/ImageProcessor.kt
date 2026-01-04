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

import java.util.concurrent.Executors

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

        // Use tiled processing for very large images to reduce peak memory
        val pixelCount = width.toLong() * height.toLong()
        val tileRowLimit = 4_000_000L // target ~4M pixels per tile (adjustable)
        if (pixelCount <= tileRowLimit) {
            // small enough — process whole image in parallel as before
            val pixels = IntArray(width * height)
            out.getPixels(pixels, 0, width, 0, 0, width, height)

            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val executor = Executors.newFixedThreadPool(cores)
            try {
                val chunkSize = (pixels.size + cores - 1) / cores
                val futures = mutableListOf<java.util.concurrent.Future<*>>()
                for (t in 0 until cores) {
                    val start = t * chunkSize
                    val end = kotlin.math.min(start + chunkSize, pixels.size)
                    if (start >= end) continue
                    futures += executor.submit {
                        var i = start
                        while (i < end) {
                            val c = pixels[i]
                            val a = Color.alpha(c)
                            val r = Color.red(c)
                            val g = Color.green(c)
                            val b = Color.blue(c)
                            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                            val shFactor = (1f - lum)
                            val hiFactor = lum
                            val newR = (r / 255f * (1f - strength) + (shR * shFactor + hiR * hiFactor) * strength) * 255f
                            val newG = (g / 255f * (1f - strength) + (shG * shFactor + hiG * hiFactor) * strength) * 255f
                            val newB = (b / 255f * (1f - strength) + (shB * shFactor + hiB * hiFactor) * strength) * 255f
                            pixels[i] = Color.argb(a, newR.coerceIn(0f, 255f).toInt(), newG.coerceIn(0f, 255f).toInt(), newB.coerceIn(0f, 255f).toInt())
                            i++
                        }
                    }
                }
                for (f in futures) f.get()
            } finally {
                executor.shutdown()
            }
            out.setPixels(pixels, 0, width, 0, 0, width, height)
            return out
        } else {
            // Large image: process in row tiles to limit memory per tile
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val targetTilePixels = tileRowLimit
            val tileHeight = kotlin.math.max(1, (targetTilePixels / width).toInt())
            val executor = Executors.newFixedThreadPool(cores)
            try {
                val tasks = mutableListOf<java.util.concurrent.Future<*>>()
                var rowStart = 0
                while (rowStart < height) {
                    val rows = kotlin.math.min(tileHeight, height - rowStart)
                    // Submit a task to process this tile
                    tasks += executor.submit {
                        val tilePixels = IntArray(width * rows)
                        out.getPixels(tilePixels, 0, width, 0, rowStart, width, rows)
                        // process this tile's pixels
                        var i = 0
                        while (i < tilePixels.size) {
                            val c = tilePixels[i]
                            val a = Color.alpha(c)
                            val r = Color.red(c)
                            val g = Color.green(c)
                            val b = Color.blue(c)
                            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                            val shFactor = (1f - lum)
                            val hiFactor = lum
                            val newR = (r / 255f * (1f - strength) + (shR * shFactor + hiR * hiFactor) * strength) * 255f
                            val newG = (g / 255f * (1f - strength) + (shG * shFactor + hiG * hiFactor) * strength) * 255f
                            val newB = (b / 255f * (1f - strength) + (shB * shFactor + hiB * hiFactor) * strength) * 255f
                            tilePixels[i] = Color.argb(a, newR.coerceIn(0f, 255f).toInt(), newG.coerceIn(0f, 255f).toInt(), newB.coerceIn(0f, 255f).toInt())
                            i++
                        }
                        // write back this tile
                        out.setPixels(tilePixels, 0, width, 0, rowStart, width, rows)
                    }
                    rowStart += rows
                }
                // wait for all tiles
                for (t in tasks) t.get()
            } finally {
                executor.shutdown()
            }
            return out
        }
    }

    // Placeholder for enhance (sharpen/denoise) - currently no-op
    fun applyEnhanceParams(src: Bitmap, params: EnhanceParams): Bitmap {
        var out = src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val paint = Paint()
        paint.isFilterBitmap = true

        // Exposure: simple brightness offset applied to RGB
        if (params.exposure != 0f) {
            val exposureOffset = (params.exposure.coerceIn(-1f, 1f) * 255f)
            val expMat = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, exposureOffset,
                0f, 1f, 0f, 0f, exposureOffset,
                0f, 0f, 1f, 0f, exposureOffset,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(expMat)
            canvas.drawBitmap(out, 0f, 0f, paint)
            paint.colorFilter = null
        }

        // Vibrance: increase saturation but protect skin tones roughly by attenuating for high-red pixels
        if (params.vibrance != 0f) {
            val v = params.vibrance.coerceIn(-1f, 1f)
            // approximate by adjusting saturation globally with weaker effect for high-red pixels via pixel loop
            val satFactor = 1f + 0.6f * v
            if (satFactor != 1f) {
                val cm = ColorMatrix()
                cm.setSaturation(satFactor.coerceAtLeast(0f))
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(out, 0f, 0f, paint)
                paint.colorFilter = null
            }
        }

        // Warmth / temperature: shift blue/red channels
        if (params.warmth != 0f) {
            val w = params.warmth.coerceIn(-1f, 1f)
            val redScale = 1f + w * 0.15f
            val blueScale = 1f - w * 0.15f
            val tempMat = ColorMatrix(floatArrayOf(
                redScale, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, blueScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(tempMat)
            canvas.drawBitmap(out, 0f, 0f, paint)
            paint.colorFilter = null
        }

        // Shadows: simple curve-like lift for dark pixels
        if (params.shadows != 0f) {
            val s = params.shadows.coerceIn(-1f, 1f)
            val pixels = IntArray(out.width * out.height)
            out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
            var i = 0
            while (i < pixels.size) {
                val c = pixels[i]
                val a = Color.alpha(c)
                var r = Color.red(c)
                var g = Color.green(c)
                var b = Color.blue(c)
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val lift = (if (s > 0f) (1f - lum) * 0.2f * s else -lum * 0.2f * (-s))
                r = (r + lift * 255f).toInt().coerceIn(0, 255)
                g = (g + lift * 255f).toInt().coerceIn(0, 255)
                b = (b + lift * 255f).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(a, r, g, b)
                i++
            }
            out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        }

        // Dehaze (dispersion): approximate by increasing midtone contrast
        if (params.dehaze != 0f) {
            val d = params.dehaze.coerceIn(0f, 1f)
            val contrast = 1f + 0.25f * d
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val cm = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(out, 0f, 0f, paint)
            paint.colorFilter = null
        }

        // Clarity: a lightweight local contrast boost using a crude unsharp mask (blur and subtract)
        if (params.clarity > 0f) {
            val c = params.clarity.coerceIn(0f, 1f)
            try {
                val blurred = StylizedFilters.blur(
                    out,
                    intensity = (4 * c).coerceAtLeast(1f)
                )
                // unsharp: out + (out - blurred) * strength
                val width = out.width
                val height = out.height
                val srcPixels = IntArray(width * height)
                val blurPixels = IntArray(width * height)
                out.getPixels(srcPixels, 0, width, 0, 0, width, height)
                blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)
                var i = 0
                val strength = 0.5f * c
                while (i < srcPixels.size) {
                    val a = Color.alpha(srcPixels[i])
                    val sr = Color.red(srcPixels[i])
                    val sg = Color.green(srcPixels[i])
                    val sb = Color.blue(srcPixels[i])
                    val br = Color.red(blurPixels[i])
                    val bg = Color.green(blurPixels[i])
                    val bb = Color.blue(blurPixels[i])
                    val nr = (sr + (sr - br) * strength).toInt().coerceIn(0, 255)
                    val ng = (sg + (sg - bg) * strength).toInt().coerceIn(0, 255)
                    val nb = (sb + (sb - bb) * strength).toInt().coerceIn(0, 255)
                    srcPixels[i] = Color.argb(a, nr, ng, nb)
                    i++
                }
                out.setPixels(srcPixels, 0, width, 0, 0, width, height)
            } catch (_: Throwable) {
                // fall back silently if blur fails
            }
        }

        // Grain: overlay procedural noise
        if (params.grain > 0f) {
            val g = params.grain.coerceIn(0f, 1f)
            val overlay = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
            val canv = Canvas(overlay)
            val rnd = java.util.Random(0)
            val paintG = Paint()
            val alpha = (g * 80).toInt().coerceIn(0, 255)
            val w = out.width
            val h = out.height
            val pixels = IntArray(w * h)
            var i = 0
            while (i < pixels.size) {
                val n = ((rnd.nextFloat() * 2f - 1f) * 255f).toInt().coerceIn(-255, 255)
                val v = (128 + n).coerceIn(0, 255)
                pixels[i] = Color.argb(alpha, v, v, v)
                i++
            }
            overlay.setPixels(pixels, 0, w, 0, 0, w, h)
            canv.drawBitmap(overlay, 0f, 0f, paintG)
            val finalPaint = Paint()
            finalPaint.isFilterBitmap = true
            finalPaint.alpha = alpha
            canvas.drawBitmap(overlay, 0f, 0f, finalPaint)
            overlay.recycle()
        }

        // Sharpen: small unsharp-ish pass
        if (params.sharpen > 0f) {
            // approximate via small unsharp: combine original and a slightly blurred version
            try {
                val s = params.sharpen.coerceIn(0f, 1f)
                val blurred = StylizedFilters.blur(
                    out,
                    intensity = (1f + 3f * s)
                )
                val width = out.width
                val height = out.height
                val srcPixels = IntArray(width * height)
                val blurPixels = IntArray(width * height)
                out.getPixels(srcPixels, 0, width, 0, 0, width, height)
                blurred.getPixels(blurPixels, 0, width, 0, 0, width, height)
                var i = 0
                val strength = 0.7f * s
                while (i < srcPixels.size) {
                    val a = Color.alpha(srcPixels[i])
                    val sr = Color.red(srcPixels[i])
                    val sg = Color.green(srcPixels[i])
                    val sb = Color.blue(srcPixels[i])
                    val br = Color.red(blurPixels[i])
                    val bg = Color.green(blurPixels[i])
                    val bb = Color.blue(blurPixels[i])
                    val nr = (sr + (sr - br) * strength).toInt().coerceIn(0, 255)
                    val ng = (sg + (sg - bg) * strength).toInt().coerceIn(0, 255)
                    val nb = (sb + (sb - bb) * strength).toInt().coerceIn(0, 255)
                    srcPixels[i] = Color.argb(a, nr, ng, nb)
                    i++
                }
                out.setPixels(srcPixels, 0, width, 0, 0, width, height)
            } catch (_: Throwable) {
                // ignore
            }
        }

        // Denoise: simplistic blur reduction based on denoise strength
        if (params.denoise > 0f) {
            try {
                val d = params.denoise.coerceIn(0f, 1f)
                val radius = (3f * d).coerceAtLeast(0.5f)
                val den = StylizedFilters.blur(
                    out,
                    intensity = radius
                )
                den?.let {
                    out.recycle()
                    out = den
                }
            } catch (_: Throwable) {
            }
        }

        return out
    }

    // Compose all processing steps using the EditorState
    // highQuality: when true, use higher-quality (and heavier) operations where available (e.g. blur)
    fun processAll(state: EditorState, highQuality: Boolean = false): Bitmap? {
        val base = state.baseBitmap ?: return null
        var bmp = base
        // Apply basic filter params
        bmp = applyFilterParams(bmp, state.filter)

        // Apply optional stylized filter if requested in FilterParams
        val stylizeId = state.filter.stylize
        if (!stylizeId.isNullOrBlank()) {
            try {
                bmp = when (stylizeId) {
                    "retro" -> StylizedFilters.retro(bmp, state.filter.stylizeIntensity)
                    "vhs" -> StylizedFilters.vhs(bmp, state.filter.stylizeIntensity)
                    "polaroid" -> StylizedFilters.polaroid(bmp, state.filter.stylizeIntensity)
                    "year90" -> StylizedFilters.year90(bmp, state.filter.stylizeIntensity)
                    "grain" -> StylizedFilters.grain(bmp, state.filter.stylizeIntensity)
                    "blur" -> if (highQuality) StylizedFilters.blurHighQuality(bmp, state.filter.stylizeIntensity) else StylizedFilters.blur(bmp, state.filter.stylizeIntensity)
                    "film" -> StylizedFilters.film(bmp, state.filter.stylizeIntensity)
                    else -> bmp
                }
            } catch (_: Throwable) {
                // ignore stylize errors and continue
            }
        }
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
