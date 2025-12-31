package com.devsapiens.phonemagic.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.devsapiens.phonemagic.processor.ImageProcessor
import com.devsapiens.phonemagic.viewmodel.EditorViewModel
import com.devsapiens.phonemagic.ui.theme.Primary
import com.devsapiens.phonemagic.ui.theme.Secondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onExport: () -> Unit, onBack: () -> Unit, viewModel: EditorViewModel) {
    val state = viewModel.state.collectAsState()

    // local state for bottom sheet visibility and selected tab
    var showFilters by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("A") } // "A" or "B"

    // Helper to build a ColorMatrix from FilterA params
    fun colorMatrixForFilterA(intensity: Float, warmth: Float, vignette: Float): ColorMatrix {
        // Start with identity
        val matrix = ColorMatrix()
        // Apply warmth: scale red up and blue down based on warmth (-1..1)
        val redScale = 1f + (warmth * 0.25f) // small red boost
        val blueScale = 1f - (warmth * 0.25f)
        val greenScale = 1f
        // Apply intensity: lerp between identity and warm scale
        val i = intensity.coerceIn(0f, 1f)
        matrix.setToScale(
            1f + (redScale - 1f) * i,
            1f + (greenScale - 1f) * i,
            1f + (blueScale - 1f) * i,
            1f
        )
        // Slight saturation shift linked to intensity
        val sat = 1f + 0.35f * i
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(sat)
        matrix.timesAssign(satMatrix)
        return matrix
    }

    // Produce a processed preview bitmap (Filter A + B) off the UI thread when baseBitmap is available
    val previewImageBitmap by produceState(initialValue = null as androidx.compose.ui.graphics.ImageBitmap?, key1 = state.value.baseBitmap, key2 = state.value.filterA, key3 = state.value.filterB) {
        val base = state.value.baseBitmap
        if (base == null) {
            value = null
        } else {
            // Do processing on a background dispatcher and scale down for preview
            val processed = withContext(Dispatchers.Default) {
                // scale down if large (max dimension 800)
                val maxDim = 800
                val src = if (kotlin.math.max(base.width, base.height) > maxDim) {
                    val scale = maxDim.toFloat() / kotlin.math.max(base.width, base.height)
                    Bitmap.createScaledBitmap(base, (base.width * scale).toInt(), (base.height * scale).toInt(), true)
                } else base
                // create a temporary EditorState-like object by copying current state but replacing baseBitmap
                val tempState = state.value.copy(baseBitmap = src)
                ImageProcessor.processAll(tempState)
            }
            value = processed?.asImageBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Editor") }, navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Back")
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onExport() }) {
                Text("Export")
            }
        }
    ) { inner ->
        Box(modifier = Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))

                // Preview card with color filter and vignette overlay
                Card(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.06f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    // If we have a processed preview bitmap, show it (combines Filter A & B). Otherwise fall back to uri painter using Filter A only
                    if (previewImageBitmap != null) {
                        androidx.compose.foundation.Image(bitmap = previewImageBitmap!!, contentDescription = "Preview", modifier = Modifier.size(300.dp))
                    } else {
                        val painter: Painter? = state.value.imageUri?.let { rememberAsyncImagePainter(it) }
                        if (painter != null) {
                            val fm = state.value.filterA
                            val cm = colorMatrixForFilterA(fm.intensity, fm.warmth, fm.vignette)
                            val cf = ColorFilter.colorMatrix(cm)

                            Box(modifier = Modifier
                                .size(300.dp)
                                .drawWithContent {
                                    drawContent()
                                    // draw vignette overlay: radial gradient from transparent center to black edges
                                    val vignetteStrength = fm.vignette.coerceIn(0f, 1f)
                                    if (vignetteStrength > 0f) {
                                        val brush = Brush.radialGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f * vignetteStrength)),
                                            center = this.size.center,
                                            radius = this.size.minDimension * 0.6f
                                        )
                                        drawRect(brush = brush)
                                    }
                                }) {
                                androidx.compose.foundation.Image(painter = painter, contentDescription = "Preview",
                                    modifier = Modifier.size(300.dp), colorFilter = cf)
                            }
                        } else {
                            Surface(modifier = Modifier.size(300.dp), color = Color.Transparent) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("No image", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Controls card
                Card(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Controles", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Button(onClick = { viewModel.undo() }) { Text("Undo") }
                            androidx.compose.material3.Button(onClick = { viewModel.redo() }) { Text("Redo") }
                            androidx.compose.material3.Button(onClick = { viewModel.reset() }) { Text("Reset") }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Quick open Filters bottom sheet
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            androidx.compose.material3.Button(onClick = { showFilters = true; activeTab = "A" }) { Text("Filtro A") }
                            androidx.compose.material3.Button(onClick = { showFilters = true; activeTab = "B" }) { Text("Filtro B") }
                            androidx.compose.material3.Button(onClick = { showFilters = true; activeTab = "A" }) { Text("Abrir Filtros") }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Stickers / Text simple placeholders
                        Row {
                            androidx.compose.material3.Button(onClick = {
                                viewModel.addLayer(com.devsapiens.phonemagic.model.Layer.Sticker(resId = android.R.drawable.star_on, x = 50f, y = 50f, scale = 1f, rotation = 0f))
                            }) { Text("Agregar sticker") }
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.material3.Button(onClick = { viewModel.addLayer(com.devsapiens.phonemagic.model.Layer.Text(text = "Hola", color = 0xFF000000.toInt(), sizeSp = 18f, x = 100f, y = 100f, rotation = 0f)) }) { Text("Agregar texto") }
                        }
                    }
                }
            }

            // Bottom-sheet like overlay implemented with a Box aligned to bottom
            if (showFilters) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { showFilters = false }
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color(0xFF0F1720))
                            .padding(16.dp)
                    ) {
                        // Sheet header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Filtros", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            androidx.compose.material3.Button(onClick = { showFilters = false }) { Text("Cerrar") }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Tabs: A or B (simple)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            androidx.compose.material3.Button(onClick = { activeTab = "A" }) { Text("A") }
                            androidx.compose.material3.Button(onClick = { activeTab = "B" }) { Text("B") }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (activeTab == "A") {
                            // Filter A controls inside sheet
                            Text("Intensity: ${String.format("%.2f", state.value.filterA.intensity)}", color = Color.White)
                            Slider(value = state.value.filterA.intensity, onValueChange = { viewModel.applyFilterA(state.value.filterA.copy(intensity = it)) }, valueRange = 0f..1f)
                            Spacer(Modifier.height(8.dp))

                            Text("Warmth: ${String.format("%.2f", state.value.filterA.warmth)}", color = Color.White)
                            Slider(value = state.value.filterA.warmth, onValueChange = { viewModel.applyFilterA(state.value.filterA.copy(warmth = it)) }, valueRange = -1f..1f)
                            Spacer(Modifier.height(8.dp))

                            Text("Vignette: ${String.format("%.2f", state.value.filterA.vignette)}", color = Color.White)
                            Slider(value = state.value.filterA.vignette, onValueChange = { viewModel.applyFilterA(state.value.filterA.copy(vignette = it)) }, valueRange = 0f..1f)
                        } else {
                            // Filter B controls
                            Text("Strength: ${String.format("%.2f", state.value.filterB.strength)}", color = Color.White)
                            Slider(value = state.value.filterB.strength, onValueChange = { viewModel.applyFilterB(state.value.filterB.copy(strength = it)) }, valueRange = 0f..1f)
                            Spacer(Modifier.height(8.dp))

                            Text("Highlights tint", color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val presets = listOf(0xFFFFFF, 0xFFE3B7, 0xA7F3D0, 0xBDE0FF)
                                presets.forEach { colorInt ->
                                    Box(modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(colorInt))
                                        .clickable { viewModel.applyFilterB(state.value.filterB.copy(highlightsTint = colorInt)) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Shadows tint", color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val presets = listOf(0x000000, 0x423F3E, 0x082F2E, 0x2B1B3D)
                                presets.forEach { colorInt ->
                                    Box(modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(colorInt))
                                        .clickable { viewModel.applyFilterB(state.value.filterB.copy(shadowsTint = colorInt)) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            androidx.compose.material3.Button(onClick = {
                                // reset filters
                                viewModel.applyFilterA(com.devsapiens.phonemagic.model.FilterAParams())
                                viewModel.applyFilterB(com.devsapiens.phonemagic.model.FilterBParams())
                            }) { Text("Reset") }
                            androidx.compose.material3.Button(onClick = { showFilters = false }) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}
