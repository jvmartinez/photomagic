package com.devsapiens.phonemagic.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.devsapiens.phonemagic.R
import com.devsapiens.phonemagic.filter.FILTER_PRESETS
import com.devsapiens.phonemagic.processor.ImageProcessor
import com.devsapiens.phonemagic.ui.theme.Primary
import com.devsapiens.phonemagic.ui.theme.Secondary
import com.devsapiens.phonemagic.util.DiskLruImageCache
import com.devsapiens.phonemagic.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.mutableStateMapOf
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onExport: () -> Unit, onBack: () -> Unit, viewModel: EditorViewModel) {
    val state = viewModel.state.collectAsState()

    // local state for bottom sheet visibility and selected tab
    var showFilters by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("A") } // "A", "B" or "Categories"

    val coroutineScope = rememberCoroutineScope()

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

    // Debounced preview cache: update previewImage when baseBitmap/filterA/filterB/layers change, debounced by 250ms
    val previewImageState = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(
        state.value.baseBitmap,
        state.value.filterA,
        state.value.filterB,
        state.value.layers
    ) {
        snapshotFlow { Triple(state.value.baseBitmap, state.value.filterA, state.value.filterB) }
            .debounce(250)
            .collectLatest { triple ->
                val baseAny = triple.first
                val baseBitmap = baseAny as? Bitmap
                if (baseBitmap == null) {
                    previewImageState.value = null
                } else {
                    val processed = withContext(Dispatchers.Default) {
                        val maxDim = 800
                        val src =
                            if (kotlin.math.max(baseBitmap.width, baseBitmap.height) > maxDim) {
                                val scale = maxDim.toFloat() / kotlin.math.max(
                                    baseBitmap.width,
                                    baseBitmap.height
                                )
                                Bitmap.createScaledBitmap(
                                    baseBitmap,
                                    (baseBitmap.width * scale).toInt(),
                                    (baseBitmap.height * scale).toInt(),
                                    true
                                )
                            } else baseBitmap
                        val tempState = state.value.copy(baseBitmap = src)
                        ImageProcessor.processAll(tempState)
                    }
                    previewImageState.value = processed?.asImageBitmap()
                }
            }
    }

    // LRU cache for preset thumbnails (memory-limited)
    val presetCache = remember {
        // allow ~40 entries by default — thumbnails are small (160x)
        LruCache<String, ImageBitmap>(40)
    }

    // Disk-backed LRU cache for persistent thumbnails
    val ctx = LocalContext.current
    val diskCache = remember {
        DiskLruImageCache(
            File(ctx.cacheDir, "preset_thumbs"),
            maxSizeBytes = 50L * 1024L * 1024L
        )
    }

    // In-memory cache for processed drawable-based previews per preset (fast placeholder)
    val drawablePreviewCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Generate processed drawable previews once (small, fast). Runs once per composition.
    LaunchedEffect(Unit) {
        val res = ctx.resources
        val raw = withContext(Dispatchers.IO) {
            // decode resource explicitly requesting ARGB_8888 to avoid config issues
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            BitmapFactory.decodeResource(res, R.drawable.ic_preview_filter, opts)
        }
        if (raw != null) {
            // Normalize to ARGB_8888 and scale to a small preview size to avoid OOM and ensure processing works
            val maxDim = 160
            val basePreview = withContext(Dispatchers.Default) {
                val scale = maxDim.toFloat() / kotlin.math.max(raw.width, raw.height).coerceAtLeast(1)
                val w = (raw.width * scale).toInt().coerceAtLeast(1)
                val h = (raw.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                // ensure ARGB_8888 config
                if (scaled.config == Bitmap.Config.ARGB_8888) scaled else scaled.copy(Bitmap.Config.ARGB_8888, true)
            }
            if (basePreview != null) {
                withContext(Dispatchers.Default) {
                    for (preset in FILTER_PRESETS) {
                        try {
                            val tempState = viewModel.state.value.copy(
                                baseBitmap = basePreview,
                                filter = preset.filter ?: viewModel.state.value.filter,
                                filterA = preset.filterA ?: viewModel.state.value.filterA,
                                filterB = preset.filterB ?: viewModel.state.value.filterB
                            )
                            var out: Bitmap? = null
                            try {
                                out = ImageProcessor.processAll(tempState)
                            } catch (e: Throwable) {
                                Log.w("EditorScreen", "processAll failed for preset ${preset.id}: ${e.message}")
                                out = null
                            }
                            if (out == null) {
                                // fallback: apply sub-steps to ensure we have a result
                                try {
                                    var bmp = basePreview
                                    bmp = ImageProcessor.applyFilterParams(bmp, tempState.filter)
                                    bmp = ImageProcessor.applyFilterAParams(bmp, tempState.filterA)
                                    bmp = ImageProcessor.applyFilterBParams(bmp, tempState.filterB)
                                    out = bmp
                                } catch (e: Throwable) {
                                    Log.e("EditorScreen", "fallback processing failed for preset ${preset.id}: ${e.message}")
                                    out = null
                                }
                            }

                            if (out != null) {
                                drawablePreviewCache[preset.id] = out.asImageBitmap()
                                Log.d("EditorScreen", "Generated drawable preview for ${preset.id}")
                            } else {
                                Log.w("EditorScreen", "Could not generate drawable preview for ${preset.id}")
                            }
                        } catch (e: Throwable) {
                            Log.e("EditorScreen", "Unexpected error processing preset ${preset.id}: ${e.message}")
                        }
                    }
                }
            }
        } else {
            Log.w("EditorScreen", "Failed to decode drawable R.drawable.ic_preview_filter")
        }
    }

    // State for thumbnails and generation flag
    val isGeneratingPreviews = remember { mutableStateOf(false) }
    val presetPreviewsState = remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    // Generate or load thumbnails (debounced by baseBitmap change)
    LaunchedEffect(state.value.baseBitmap) {
        val base = state.value.baseBitmap
        if (base == null) {
            presetPreviewsState.value = emptyMap()
            isGeneratingPreviews.value = false
        } else {
            isGeneratingPreviews.value = true
            val map = mutableMapOf<String, ImageBitmap>()
            withContext(Dispatchers.IO) {
                val thumbDim = 160
                val src = if (kotlin.math.max(base.width, base.height) > thumbDim) {
                    val scale = thumbDim.toFloat() / kotlin.math.max(base.width, base.height)
                    Bitmap.createScaledBitmap(
                        base,
                        (base.width * scale).toInt(),
                        (base.height * scale).toInt(),
                        true
                    )
                } else base

                for (preset in FILTER_PRESETS) {
                    // Try memory cache first
                    val mem = presetCache.get(preset.id)
                    if (mem != null) {
                        map[preset.id] = mem
                        continue
                    }

                    // Try disk cache
                    val diskBitmap = diskCache.getBitmap(preset.id)
                    if (diskBitmap != null) {
                        val ib = diskBitmap.asImageBitmap()
                        presetCache.put(preset.id, ib)
                        map[preset.id] = ib
                        continue
                    }

                    // Not cached: generate thumbnail
                    val tempState = state.value.copy(
                        baseBitmap = src,
                        filter = preset.filter ?: state.value.filter,
                        filterA = preset.filterA ?: state.value.filterA,
                        filterB = preset.filterB ?: state.value.filterB
                    )
                    val out = ImageProcessor.processAll(tempState)
                    if (out != null) {
                        // write to disk and memory cache
                        diskCache.putBitmap(preset.id, out)
                        val ib = out.asImageBitmap()
                        presetCache.put(preset.id, ib)
                        map[preset.id] = ib
                    }
                }
            }
            presetPreviewsState.value = map
            isGeneratingPreviews.value = false
        }
    }

    // Expose presetPreviews as an immutable map
    val presetPreviews = presetPreviewsState.value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Editor") }, navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Back"
                    )
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onExport() }) {
                Text("Export")
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))

                // Preview card with color filter and vignette overlay
                Card(
                    modifier = Modifier
                        .size(400.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.06f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    val previewImageBitmap = previewImageState.value
                    if (previewImageBitmap != null) {
                        Image(
                            bitmap = previewImageBitmap,
                            contentDescription = "Preview",
                            modifier = Modifier.size(300.dp)
                        )
                    } else {
                        val painter: Painter? =
                            state.value.imageUri?.let { rememberAsyncImagePainter(it) }
                        if (painter != null) {
                            val fm = state.value.filterA
                            val cm = colorMatrixForFilterA(fm.intensity, fm.warmth, fm.vignette)
                            val cf = ColorFilter.colorMatrix(cm)

                            Box(
                                modifier = Modifier
                                    .size(400.dp)
                                    .drawWithContent {
                                        drawContent()
                                        val vignetteStrength = fm.vignette.coerceIn(0f, 1f)
                                        if (vignetteStrength > 0f) {
                                            val brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.6f * vignetteStrength)
                                                ),
                                                center = this.size.center,
                                                radius = this.size.minDimension * 0.6f
                                            )
                                            drawRect(brush = brush)
                                        }
                                    }) {
                                Image(
                                    painter = painter, contentDescription = "Preview",
                                    modifier = Modifier.size(400.dp), colorFilter = cf
                                )
                            }
                        } else {
                            Surface(modifier = Modifier.size(300.dp), color = Color.Transparent) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
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

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Button(onClick = { viewModel.undo() }) {
                                Text(
                                    "Undo"
                                )
                            }
                            androidx.compose.material3.Button(onClick = { viewModel.redo() }) {
                                Text(
                                    "Redo"
                                )
                            }
                            androidx.compose.material3.Button(onClick = { viewModel.reset() }) {
                                Text(
                                    "Reset"
                                )
                            }
                            // Bake / Apply filters: render current state into the base bitmap and reset filters
                            var isBaking by remember { mutableStateOf(false) }
                            androidx.compose.material3.Button(onClick = {
                                isBaking = true
                                coroutineScope.launch {
                                    val baked =
                                        withContext(Dispatchers.Default) { viewModel.bakeFilters() }
                                    if (baked != null) {
                                        // replace base bitmap with baked result and reset filter params
                                        viewModel.applyBakedBitmap(baked)
                                        viewModel.applyFilterA(com.devsapiens.phonemagic.model.FilterAParams())
                                        viewModel.applyFilterB(com.devsapiens.phonemagic.model.FilterBParams())
                                    }
                                    isBaking = false
                                }
                            }) {
                                if (isBaking) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else Text("Aplicar filtros")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Quick open Filters bottom sheet
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            androidx.compose.material3.Button(onClick = {
                                showFilters = true; activeTab = "A"
                            }) { Text("Filtro A") }
                            androidx.compose.material3.Button(onClick = {
                                showFilters = true; activeTab = "B"
                            }) { Text("Filtro B") }
                            androidx.compose.material3.Button(onClick = {
                                showFilters = true; activeTab = "A"
                            }) { Text("Abrir Filtros") }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Stickers / Text simple placeholders
                        Row {
                            androidx.compose.material3.Button(onClick = {
                                viewModel.addLayer(
                                    com.devsapiens.phonemagic.model.Layer.Sticker(
                                        resId = android.R.drawable.star_on,
                                        x = 50f,
                                        y = 50f,
                                        scale = 1f,
                                        rotation = 0f
                                    )
                                )
                            }) { Text("Agregar sticker") }
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.material3.Button(onClick = {
                                viewModel.addLayer(
                                    com.devsapiens.phonemagic.model.Layer.Text(
                                        text = "Hola",
                                        color = 0xFF000000.toInt(),
                                        sizeSp = 18f,
                                        x = 100f,
                                        y = 100f,
                                        rotation = 0f
                                    )
                                )
                            }) { Text("Agregar texto") }
                        }
                    }
                }
            }

            // Bottom-sheet like overlay implemented with a Box aligned to bottom
            if (showFilters) {
                Box(
                    modifier = Modifier
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filtros",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            androidx.compose.material3.Button(onClick = {
                                showFilters = false
                            }) { Text("Cerrar") }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Tabs: A, B or Categories (simple)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            androidx.compose.material3.Button(onClick = { activeTab = "A" }) {
                                Text(
                                    "A"
                                )
                            }
                            androidx.compose.material3.Button(onClick = { activeTab = "B" }) {
                                Text(
                                    "B"
                                )
                            }
                            androidx.compose.material3.Button(onClick = {
                                activeTab = "Categories"
                            }) { Text("Categorias") }
                        }

                        Spacer(Modifier.height(12.dp))

                        when (activeTab) {
                            "A" -> {
                                // Filter A controls inside sheet
                                Text(
                                    "Intensity: ${
                                        String.format(
                                            "%.2f",
                                            state.value.filterA.intensity
                                        )
                                    }", color = Color.White
                                )
                                Slider(
                                    value = state.value.filterA.intensity,
                                    onValueChange = {
                                        viewModel.applyFilterA(
                                            state.value.filterA.copy(intensity = it)
                                        )
                                    },
                                    valueRange = 0f..1f
                                )
                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "Warmth: ${String.format("%.2f", state.value.filterA.warmth)}",
                                    color = Color.White
                                )
                                Slider(
                                    value = state.value.filterA.warmth,
                                    onValueChange = {
                                        viewModel.applyFilterA(
                                            state.value.filterA.copy(warmth = it)
                                        )
                                    },
                                    valueRange = -1f..1f
                                )
                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "Vignette: ${
                                        String.format(
                                            "%.2f",
                                            state.value.filterA.vignette
                                        )
                                    }", color = Color.White
                                )
                                Slider(
                                    value = state.value.filterA.vignette,
                                    onValueChange = {
                                        viewModel.applyFilterA(
                                            state.value.filterA.copy(vignette = it)
                                        )
                                    },
                                    valueRange = 0f..1f
                                )
                            }

                            "B" -> {
                                // Filter B controls
                                Text(
                                    "Strength: ${
                                        String.format(
                                            "%.2f",
                                            state.value.filterB.strength
                                        )
                                    }", color = Color.White
                                )
                                Slider(
                                    value = state.value.filterB.strength,
                                    onValueChange = {
                                        viewModel.applyFilterB(
                                            state.value.filterB.copy(strength = it)
                                        )
                                    },
                                    valueRange = 0f..1f
                                )
                                Spacer(Modifier.height(8.dp))

                                Text("Highlights tint", color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val presets = listOf(0xFFFFFF, 0xFFE3B7, 0xA7F3D0, 0xBDE0FF)
                                    presets.forEach { colorInt ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(colorInt))
                                                .clickable {
                                                    viewModel.applyFilterB(
                                                        state.value.filterB.copy(
                                                            highlightsTint = colorInt
                                                        )
                                                    )
                                                }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Shadows tint", color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val presets = listOf(0x000000, 0x423F3E, 0x082F2E, 0x2B1B3D)
                                    presets.forEach { colorInt ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(colorInt))
                                                .clickable {
                                                    viewModel.applyFilterB(
                                                        state.value.filterB.copy(
                                                            shadowsTint = colorInt
                                                        )
                                                    )
                                                }
                                        )
                                    }
                                }
                            }

                            "Categories" -> {
                                // Categories tab showing preset thumbnails
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                ) {
                                    // Grid or list of preset thumbnails
                                    val presets = FILTER_PRESETS
                                    LazyHorizontalGrid(
                                        rows = GridCells.Adaptive(minSize = 128.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(presets) { preset ->
                                            val bitmap = presetPreviews[preset.id]
                                            val isSelected = false
                                            Box(
                                                modifier = Modifier
                                                    .size(128.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Primary else Secondary)
                                                    .clickable {
                                                        viewModel.applyFilterA(
                                                            preset.filterA ?: state.value.filterA
                                                        )
                                                        viewModel.applyFilterB(
                                                            preset.filterB ?: state.value.filterB
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (bitmap != null) {
                                                    Image(
                                                        bitmap = bitmap,
                                                        contentDescription = preset.displayName,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    // Show processed drawable preview if available; otherwise static drawable
                                                    val processedDrawable =
                                                        drawablePreviewCache[preset.id]
//                                                    if (processedDrawable != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(0xFF1E293B))
                                                        ) {
                                                            processedDrawable?.let {
                                                                Image(
                                                                    bitmap = it,
                                                                    contentDescription = "preset preview",
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.align(Alignment.Center)
                                                                )
                                                            }
                                                        }
//                                                    } else {
//                                                        val previewPainter =
//                                                            painterResource(id = R.drawable.ic_preview_filter)
//                                                        Box(
//                                                            modifier = Modifier
//                                                                .fillMaxSize()
//                                                                .background(Color(0xFF1E293B))
//                                                        ) {
//                                                            Image(
//                                                                painter = previewPainter,
//                                                                contentDescription = "preset preview",
//                                                                contentScale = ContentScale.Crop,
//                                                                modifier = Modifier.align(Alignment.Center)
//                                                            )
//                                                        }
//                                                    }
                                                    // If generation in progress show a small spinner overlay on top
                                                    if (isGeneratingPreviews.value) {
                                                        Box(
                                                            modifier = Modifier.matchParentSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                color = Color.White,
                                                                strokeWidth = 2.dp
                                                            )
                                                        }
                                                    }
                                                }

                                                // Preset name label
                                                Text(
                                                    text = preset.displayName,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            androidx.compose.material3.Button(onClick = {
                                viewModel.applyFilterA(com.devsapiens.phonemagic.model.FilterAParams())
                                viewModel.applyFilterB(com.devsapiens.phonemagic.model.FilterBParams())
                            }) { Text("Reset") }
                            androidx.compose.material3.Button(onClick = {
                                showFilters = false
                            }) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}
