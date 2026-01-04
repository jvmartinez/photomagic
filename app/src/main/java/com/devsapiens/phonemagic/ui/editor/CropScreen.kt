package com.devsapiens.phonemagic.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * CropScreen mejorado: soporta arrastrar las esquinas individualmente y presets de aspect ratio.
 */

private enum class DragHandle { NONE, MOVE, TL, TR, BL, BR }

@Composable
fun CropScreen(
    bitmap: Bitmap,
    leftNormState: MutableState<Float>,
    topNormState: MutableState<Float>,
    rightNormState: MutableState<Float>,
    bottomNormState: MutableState<Float>,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val img = bitmap.asImageBitmap()
    val minSizeNorm = 0.01f
    val scope = rememberCoroutineScope()
    val aspectRatio = remember { mutableStateOf<Float?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = img,
                contentDescription = "Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            val canvasW = remember { mutableStateOf(0f) }
            val canvasH = remember { mutableStateOf(0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        canvasW.value = it.width.toFloat(); canvasH.value = it.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (canvasW.value <= 0f || canvasH.value <= 0f) return@detectTransformGestures

                            val l = leftNormState.value
                            val t = topNormState.value
                            val r = rightNormState.value
                            val b = bottomNormState.value

                            val px = centroid.x
                            val py = centroid.y

                            val lx = l * canvasW.value
                            val tx = t * canvasH.value
                            val rx = r * canvasW.value
                            val bx = b * canvasH.value

                            val threshold = 36f
                            fun dist(x1: Float, y1: Float, x2: Float, y2: Float) =
                                kotlin.math.hypot(x1 - x2, y1 - y2)

                            val dTL = dist(px, py, lx, tx)
                            val dTR = dist(px, py, rx, tx)
                            val dBL = dist(px, py, lx, bx)
                            val dBR = dist(px, py, rx, bx)

                            val inside = px in lx..rx && py in tx..bx

                            val handle = when {
                                dTL <= threshold -> DragHandle.TL
                                dTR <= threshold -> DragHandle.TR
                                dBL <= threshold -> DragHandle.BL
                                dBR <= threshold -> DragHandle.BR
                                inside -> DragHandle.MOVE
                                else -> DragHandle.NONE
                            }

                            val dxN = pan.x / canvasW.value
                            val dyN = pan.y / canvasH.value

                            // Pinch-to-scale: if significant zoom, scale rect about centroid
                            if (kotlin.math.abs(zoom - 1f) > 0.01f) {
                                val centerXN = (px / canvasW.value).coerceIn(0f, 1f)
                                val centerYN = (py / canvasH.value).coerceIn(0f, 1f)
                                val curW = r - l
                                val curH = b - t
                                var newW = (curW * zoom).coerceIn(minSizeNorm, 1f)
                                var newH = (curH * zoom).coerceIn(minSizeNorm, 1f)
                                aspectRatio.value?.let { ratio ->
                                    val targetRatio = ratio
                                    newH = (newW / targetRatio).coerceIn(minSizeNorm, 1f)
                                    newW = (newH * targetRatio).coerceIn(minSizeNorm, 1f)
                                }

                                val leftN = (centerXN - newW / 2f).coerceIn(0f, 1f - newW)
                                val topN = (centerYN - newH / 2f).coerceIn(0f, 1f - newH)
                                leftNormState.value = leftN
                                rightNormState.value = (leftN + newW).coerceIn(newW, 1f)
                                topNormState.value = topN
                                bottomNormState.value = (topN + newH).coerceIn(newH, 1f)

                                return@detectTransformGestures
                            }

                            when (handle) {
                                DragHandle.TL -> {
                                    val newL = (l + dxN).coerceIn(0f, r - minSizeNorm)
                                    val newT = (t + dyN).coerceIn(0f, b - minSizeNorm)
                                    leftNormState.value = newL
                                    topNormState.value = newT
                                    aspectRatio.value?.let { ratio ->
                                        val wNorm = rightNormState.value - leftNormState.value
                                        val desiredH = wNorm / ratio
                                        val centerY =
                                            (topNormState.value + bottomNormState.value) / 2f
                                        val halfH = desiredH / 2f
                                        topNormState.value =
                                            (centerY - halfH).coerceIn(0f, 1f - desiredH)
                                        bottomNormState.value =
                                            (centerY + halfH).coerceIn(desiredH, 1f)
                                    }
                                }

                                DragHandle.TR -> {
                                    val newR = (r + dxN).coerceIn(l + minSizeNorm, 1f)
                                    val newT = (t + dyN).coerceIn(0f, b - minSizeNorm)
                                    rightNormState.value = newR
                                    topNormState.value = newT
                                    aspectRatio.value?.let { ratio ->
                                        val wNorm = rightNormState.value - leftNormState.value
                                        val desiredH = wNorm / ratio
                                        val centerY =
                                            (topNormState.value + bottomNormState.value) / 2f
                                        val halfH = desiredH / 2f
                                        topNormState.value =
                                            (centerY - halfH).coerceIn(0f, 1f - desiredH)
                                        bottomNormState.value =
                                            (centerY + halfH).coerceIn(desiredH, 1f)
                                    }
                                }

                                DragHandle.BL -> {
                                    val newL = (l + dxN).coerceIn(0f, r - minSizeNorm)
                                    val newB = (b + dyN).coerceIn(t + minSizeNorm, 1f)
                                    leftNormState.value = newL
                                    bottomNormState.value = newB
                                    aspectRatio.value?.let { ratio ->
                                        val hNorm = bottomNormState.value - topNormState.value
                                        val desiredW = ratio * hNorm
                                        val centerX =
                                            (leftNormState.value + rightNormState.value) / 2f
                                        val halfW = desiredW / 2f
                                        leftNormState.value =
                                            (centerX - halfW).coerceIn(0f, 1f - desiredW)
                                        rightNormState.value =
                                            (centerX + halfW).coerceIn(desiredW, 1f)
                                    }
                                }

                                DragHandle.BR -> {
                                    val newR = (r + dxN).coerceIn(l + minSizeNorm, 1f)
                                    val newB = (b + dyN).coerceIn(t + minSizeNorm, 1f)
                                    rightNormState.value = newR
                                    bottomNormState.value = newB
                                    aspectRatio.value?.let { ratio ->
                                        val hNorm = bottomNormState.value - topNormState.value
                                        val desiredW = ratio * hNorm
                                        val centerX =
                                            (leftNormState.value + rightNormState.value) / 2f
                                        val halfW = desiredW / 2f
                                        leftNormState.value =
                                            (centerX - halfW).coerceIn(0f, 1f - desiredW)
                                        rightNormState.value =
                                            (centerX + halfW).coerceIn(desiredW, 1f)
                                    }
                                }

                                DragHandle.MOVE -> {
                                    val newL = (l + dxN).coerceIn(0f, 1f - (r - l))
                                    val width = r - l
                                    val height = b - t
                                    leftNormState.value = newL
                                    rightNormState.value = (newL + width).coerceIn(0f, 1f)
                                    val newT = (t + dyN).coerceIn(0f, 1f - height)
                                    topNormState.value = newT
                                    bottomNormState.value = (newT + height).coerceIn(0f, 1f)
                                }

                                else -> {}
                            }
                        }
                    }) {
                val w = size.width
                val h = size.height
                val l = leftNormState.value * w
                val t = topNormState.value * h
                val r = rightNormState.value * w
                val b = bottomNormState.value * h

                // dim overlay
                drawRect(color = Color(0x88000000))
                // outline
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(r - l, b - t),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
                // corners
                val cornerSize = 18f
                drawRect(
                    Color.Yellow,
                    topLeft = Offset(l - cornerSize / 2f, t - cornerSize / 2f),
                    size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
                )
                drawRect(
                    Color.Yellow,
                    topLeft = Offset(r - cornerSize / 2f, t - cornerSize / 2f),
                    size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
                )
                drawRect(
                    Color.Yellow,
                    topLeft = Offset(l - cornerSize / 2f, b - cornerSize / 2f),
                    size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
                )
                drawRect(
                    Color.Yellow,
                    topLeft = Offset(r - cornerSize / 2f, b - cornerSize / 2f),
                    size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)) {
                Text("Ajustar recorte", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val options = listOf<Pair<String, Float?>>(
                        "Free" to null,
                        "1:1" to 1f,
                        "4:3" to (4f / 3f),
                        "16:9" to (16f / 9f)
                    )
                    for ((label, ratio) in options) {
                        Button(onClick = {
                            scope.launch {
                                aspectRatio.value = ratio
                                // apply ratio to current rect
                                ratio?.let { r ->
                                    // adjust to fit ratio keeping center
                                    val l = leftNormState.value
                                    val t = topNormState.value
                                    val rgt = rightNormState.value
                                    val btm = bottomNormState.value
                                    val cx = (l + rgt) / 2f
                                    val cy = (t + btm) / 2f
                                    val currH = btm - t
                                    val desiredW = (r * currH).coerceIn(0.01f, 1f)
                                    // choose which keeps more area (prefer changing height)
                                    val newW = desiredW
                                    val newH = newW / r
                                    val halfW = newW / 2f
                                    val halfH = newH / 2f
                                    leftNormState.value = (cx - halfW).coerceIn(0f, 1f - newW)
                                    rightNormState.value = (cx + halfW).coerceIn(newW, 1f)
                                    topNormState.value = (cy - halfH).coerceIn(0f, 1f - newH)
                                    bottomNormState.value = (cy + halfH).coerceIn(newH, 1f)
                                }
                            }
                        }) {
                            Text(label)
                        }
                    }
                }

//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Sliders to fine-tune normalized coords
//                Text("Left: ${String.format("%.2f", leftNormState.value)}", color = Color.White)
//                androidx.compose.material3.Slider(value = leftNormState.value, onValueChange = { v -> leftNormState.value = v.coerceIn(0f, rightNormState.value - 0.01f) })
//                Text("Top: ${String.format("%.2f", topNormState.value)}", color = Color.White)
//                androidx.compose.material3.Slider(value = topNormState.value, onValueChange = { v -> topNormState.value = v.coerceIn(0f, bottomNormState.value - 0.01f) })
//                Text("Right: ${String.format("%.2f", rightNormState.value)}", color = Color.White)
//                androidx.compose.material3.Slider(value = rightNormState.value, onValueChange = { v -> rightNormState.value = v.coerceIn(leftNormState.value + 0.01f, 1f) })
//                Text("Bottom: ${String.format("%.2f", bottomNormState.value)}", color = Color.White)
//                androidx.compose.material3.Slider(value = bottomNormState.value, onValueChange = { v -> bottomNormState.value = v.coerceIn(topNormState.value + 0.01f, 1f) })

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onCancel) { Text("Cancelar") }
                    Button(onClick = onApply) { Text("Aplicar") }
                }
            }
        }
    }
}
