package com.devsapiens.phonemagic.ui.editor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import coil.compose.rememberAsyncImagePainter
import com.devsapiens.phonemagic.R
import com.devsapiens.phonemagic.component.button.ButtonWithLabelComponent
import com.devsapiens.phonemagic.component.button.TypeLabel
import com.devsapiens.phonemagic.filter.FILTER_PRESETS
import com.devsapiens.phonemagic.processor.ImageProcessor
import com.devsapiens.phonemagic.ui.theme.Coral
import com.devsapiens.phonemagic.ui.theme.Primary
import com.devsapiens.phonemagic.ui.theme.Secondary
import com.devsapiens.phonemagic.util.DiskLruImageCache
import com.devsapiens.phonemagic.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("UseKtx", "LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun EditorScreen(onExport: () -> Unit, onBack: () -> Unit, viewModel: EditorViewModel) {
    val state by viewModel.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("Categories") } // "A", "B" or "Categories"
    val coroutineScope = rememberCoroutineScope()
    val drawablePreviewCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    fun colorMatrixForFilterA(intensity: Float, warmth: Float, vignette: Float): ColorMatrix {
        val matrix = ColorMatrix()
        val redScale = 1f + (warmth * 0.25f) // small red boost
        val blueScale = 1f - (warmth * 0.25f)
        val greenScale = 1f
        val i = intensity.coerceIn(0f, 1f)
        matrix.setToScale(
            1f + (redScale - 1f) * i,
            1f + (greenScale - 1f) * i,
            1f + (blueScale - 1f) * i,
            1f
        )
        val sat = 1f + 0.35f * i
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(sat)
        matrix.timesAssign(satMatrix)
        return matrix
    }

    val previewImageState = remember { mutableStateOf<ImageBitmap?>(null) }
    val ctx = LocalContext.current

    BackHandler(true) {
        viewModel.clearState()
        viewModel.clearLayers()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state }
            .debounce(120)
            .collectLatest { s ->
                Log.d(
                    "EditorScreen",
                    "Preview update triggered - baseBitmap=${s.baseBitmap != null}, filter=${s.filter}, filterA=${s.filterA}, filterB=${s.filterB}"
                )
                val maxDim = 800
                val srcBitmap: Bitmap? = withContext(Dispatchers.IO) {
                    s.baseBitmap ?: run {
                        val uri = s.imageUri ?: return@withContext null
                        try {
                            ctx.contentResolver.openInputStream(uri)?.use { ins ->
                                val opts = BitmapFactory.Options()
                                    .apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                                val decoded =
                                    BitmapFactory.decodeStream(ins, null, opts) ?: return@use null
                                val scale = maxDim.toFloat() / kotlin.math.max(
                                    decoded.width,
                                    decoded.height
                                ).coerceAtLeast(1)
                                if (scale < 1f) Bitmap.createScaledBitmap(
                                    decoded,
                                    (decoded.width * scale).toInt(),
                                    (decoded.height * scale).toInt(),
                                    true
                                ) else decoded
                            }
                        } catch (e: Throwable) {
                            Log.w(
                                "EditorScreen",
                                "Failed to decode imageUri for preview: ${e.message}"
                            )
                            null
                        }
                    }
                }

                if (srcBitmap == null) {
                    previewImageState.value = null
                    return@collectLatest
                }

                val processed = withContext(Dispatchers.Default) {
                    val src = if (kotlin.math.max(srcBitmap.width, srcBitmap.height) > maxDim) {
                        val scale =
                            maxDim.toFloat() / kotlin.math.max(srcBitmap.width, srcBitmap.height)
                        Bitmap.createScaledBitmap(
                            srcBitmap,
                            (srcBitmap.width * scale).toInt(),
                            (srcBitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        srcBitmap
                    }

                    if (s.baseBitmap == null) {
                        try {
                            withContext(Dispatchers.Main) {
                                viewModel.setBaseBitmap(src)
                            }
                        } catch (_: Throwable) {
                        }
                    }

                    val tempState = s.copy(baseBitmap = src)
                    var out: Bitmap? = null
                    try {
                        out = ImageProcessor.processAll(tempState)
                    } catch (e: Throwable) {
                        Log.w("EditorScreen", "processAll preview failed: ${e.message}")
                        out = null
                    }

                    if (out == null) {
                        try {
                            var bmp = src
                            bmp = ImageProcessor.applyFilterParams(bmp, tempState.filter)
                            bmp = ImageProcessor.applyFilterAParams(bmp, tempState.filterA)
                            bmp = ImageProcessor.applyFilterBParams(bmp, tempState.filterB)
                            bmp = ImageProcessor.applyEnhanceParams(bmp, tempState.enhance)
                            if (tempState.layers.isNotEmpty()) {
                                val layeredState = tempState.copy(baseBitmap = bmp)
                                out = ImageProcessor.processAll(layeredState) ?: bmp
                            } else out = bmp
                        } catch (e: Throwable) {
                            Log.e(
                                "EditorScreen",
                                "fallback preview processing failed: ${e.message}"
                            )
                            out = null
                        }
                    }
                    out
                }

                previewImageState.value = processed?.asImageBitmap()
                Log.d("EditorScreen", "Preview update finished - success=${processed != null}")
            }
    }
    val presetCache = remember {
        LruCache<String, ImageBitmap>(40)
    }
    val diskCache = remember {
        DiskLruImageCache(
            File(ctx.cacheDir, "preset_thumbs"),
            maxSizeBytes = 50L * 1024L * 1024L
        )
    }

    LaunchedEffect(Unit) {
        val res = ctx.resources
        withContext(Dispatchers.Default) {
            val maxDim = 160
            for (preset in FILTER_PRESETS) {
                try {
                    // Decode the preset's preview drawable on IO
                    val raw = withContext(Dispatchers.IO) {
                        val opts = BitmapFactory.Options()
                            .apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                        BitmapFactory.decodeResource(res, preset.previewRes, opts)
                    }
                    if (raw == null) {
                        Log.w(
                            "EditorScreen",
                            "Failed to decode drawable for preset ${preset.id} (res=${preset.previewRes})"
                        )
                        continue
                    }

                    val basePreview = withContext(Dispatchers.Default) {
                        val scale = maxDim.toFloat() / kotlin.math.max(raw.width, raw.height)
                            .coerceAtLeast(1)
                        val w = (raw.width * scale).toInt().coerceAtLeast(1)
                        val h = (raw.height * scale).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                        if (scaled.config == Bitmap.Config.ARGB_8888) scaled else scaled.copy(
                            Bitmap.Config.ARGB_8888,
                            true
                        )
                    }

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
                        Log.w(
                            "EditorScreen",
                            "processAll failed for preset ${preset.id}: ${e.message}"
                        )
                        out = null
                    }

                    if (out == null) {
                        try {
                            var bmp = basePreview
                            bmp = ImageProcessor.applyFilterParams(bmp, tempState.filter)
                            bmp = ImageProcessor.applyFilterAParams(bmp, tempState.filterA)
                            bmp = ImageProcessor.applyFilterBParams(bmp, tempState.filterB)
                            out = bmp
                        } catch (e: Throwable) {
                            Log.e(
                                "EditorScreen",
                                "fallback processing failed for preset ${preset.id}: ${e.message}"
                            )
                            out = null
                        }
                    }

                    val finalBitmap = out ?: basePreview
                    drawablePreviewCache[preset.id] = finalBitmap.asImageBitmap()
                    Log.d("EditorScreen", "Generated drawable preview for ${preset.id}")

                } catch (e: Throwable) {
                    Log.e(
                        "EditorScreen",
                        "Unexpected error processing preset ${preset.id}: ${e.message}"
                    )
                }
            }
        }
    }
    val isGeneratingPreviews = remember { mutableStateOf(false) }
    val presetPreviewsState = remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(state.baseBitmap) {
        val base = state.baseBitmap
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
                    base.scale((base.width * scale).toInt(), (base.height * scale).toInt())
                } else {
                    base
                }

                for (preset in FILTER_PRESETS) {
                    val mem = presetCache.get(preset.id)
                    if (mem != null) {
                        map[preset.id] = mem
                        continue
                    }
                    val diskBitmap = diskCache.getBitmap(preset.id)
                    if (diskBitmap != null) {
                        val ib = diskBitmap.asImageBitmap()
                        presetCache.put(preset.id, ib)
                        map[preset.id] = ib
                        continue
                    }
                    val tempState = state.copy(
                        baseBitmap = src,
                        filter = preset.filter ?: state.filter,
                        filterA = preset.filterA ?: state.filterA,
                        filterB = preset.filterB ?: state.filterB
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

    val presetPreviews = presetPreviewsState.value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Editor") }, navigationIcon = {
                IconButton(onClick = {
                    viewModel.clearState()
                    viewModel.clearLayers()
                    onBack()
                }) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Back"
                    )
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = Coral,
                onClick = { onExport() }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_file_save_24),
                    contentDescription = "Save",
                )
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.04f)
                    )
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ButtonWithLabelComponent(
                                label = "Undo",
                                icon = R.drawable.ic_undo_24
                            ) {
                                viewModel.undo()
                            }
                            Spacer(Modifier.width(8.dp))
                            ButtonWithLabelComponent(
                                label = "Redo",
                                icon = R.drawable.ic_redo_24
                            ) {
                                viewModel.redo()
                            }
                            Spacer(Modifier.width(8.dp))
                            ButtonWithLabelComponent(
                                label = "Reset",
                                icon = R.drawable.ic_reset_image_24
                            ) {
                                viewModel.reset()
                            }
                            Spacer(Modifier.width(8.dp))
                            var isBaking by remember { mutableStateOf(false) }
                            if (isBaking) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )

                            } else {
                                ButtonWithLabelComponent(
                                    label = "Aplicar",
                                    icon = R.drawable.ic_check_small_24
                                ) {
                                    isBaking = true
                                    coroutineScope.launch {
                                        viewModel.bakeFilters()?.let { baked ->
//                                            viewModel.applyBakedBitmap(baked)
                                            viewModel.applyFilterA(com.devsapiens.phonemagic.model.FilterAParams())
                                            viewModel.applyFilterB(com.devsapiens.phonemagic.model.FilterBParams())
                                        }
                                        isBaking = false
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.06f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    val previewImageBitmap = previewImageState.value
                    if (previewImageBitmap != null) {
                        Image(
                            bitmap = previewImageBitmap,
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val painter: Painter? =
                            state.imageUri?.let { rememberAsyncImagePainter(it) }
                        if (painter != null) {
                            val fm = state.filterA
                            val cm = colorMatrixForFilterA(fm.intensity, fm.warmth, fm.vignette)
                            val cf = ColorFilter.colorMatrix(cm)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                    }
                            ) {
                                Image(
                                    painter = painter,
                                    contentDescription = "Preview",
                                    colorFilter = cf,
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

                Spacer(Modifier.width(8.dp))
                if (showFilters) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(Color(0xFF0F1720))
                            .padding(16.dp)
                    ) {
                        ButtonWithLabelComponent(
                            label = "Cerrar filtros",
                            icon = R.drawable.ic_cancel_24,
                            typeLabel = TypeLabel.Horizontal
                        ) {
                            showFilters = false
                        }

//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceBetween,
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Text(
//                                        "Filtros",
//                                        style = MaterialTheme.typography.titleLarge,
//                                        color = Color.White
//                                    )
//                                    androidx.compose.material3.Button(onClick = {
//                                        showFilters = false
//                                    }) { Text("Cerrar") }
//                                }

//                                Spacer(Modifier.height(12.dp))

//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceEvenly
//                                ) {
//                                    Button(onClick = { activeTab = "A" }) {
//                                        Text(
//                                            "A"
//                                        )
//                                    }
//                                    Button(onClick = { activeTab = "B" }) {
//                                        Text(
//                                            "B"
//                                        )
//                                    }
//                                    Button(onClick = {
//                                        activeTab = "Categories"
//                                    }) { Text("Categorias") }
//                                }
//                                when (activeTab) {
//                                    "A" -> {
//                                        Text(
//                                            "Intensity: ${
//                                                String.format(
//                                                    "%.2f",
//                                                    state.filterA.intensity
//                                                )
//                                            }", color = Color.White
//                                        )
//                                        Slider(
//                                            value = state.filterA.intensity,
//                                            onValueChange = {
//                                                viewModel.applyFilterA(
//                                                    state.filterA.copy(intensity = it)
//                                                )
//                                            },
//                                            valueRange = 0f..1f
//                                        )
//                                        Spacer(Modifier.height(8.dp))
//
//                                        Text(
//                                            "Warmth: ${String.format("%.2f", state.filterA.warmth)}",
//                                            color = Color.White
//                                        )
//                                        Slider(
//                                            value = state.filterA.warmth,
//                                            onValueChange = {
//                                                viewModel.applyFilterA(
//                                                    state.filterA.copy(warmth = it)
//                                                )
//                                            },
//                                            valueRange = -1f..1f
//                                        )
//                                        Spacer(Modifier.height(8.dp))
//
//                                        Text(
//                                            "Vignette: ${
//                                                String.format(
//                                                    "%.2f",
//                                                    state.filterA.vignette
//                                                )
//                                            }", color = Color.White
//                                        )
//                                        Slider(
//                                            value = state.filterA.vignette,
//                                            onValueChange = {
//                                                viewModel.applyFilterA(
//                                                    state.filterA.copy(vignette = it)
//                                                )
//                                            },
//                                            valueRange = 0f..1f
//                                        )
//                                    }
//
//                                    "B" -> {
//                                        // Filter B controls
//                                        Text(
//                                            "Strength: ${
//                                                String.format(
//                                                    "%.2f",
//                                                    state.filterB.strength
//                                                )
//                                            }", color = Color.White
//                                        )
//                                        Slider(
//                                            value = state.filterB.strength,
//                                            onValueChange = {
//                                                viewModel.applyFilterB(
//                                                    state.filterB.copy(strength = it)
//                                                )
//                                            },
//                                            valueRange = 0f..1f
//                                        )
//                                        Spacer(Modifier.height(8.dp))
//
//                                        Text("Highlights tint", color = Color.White)
//                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                            val presets = listOf(0xFFFFFF, 0xFFE3B7, 0xA7F3D0, 0xBDE0FF)
//                                            presets.forEach { colorInt ->
//                                                Box(
//                                                    modifier = Modifier
//                                                        .size(36.dp)
//                                                        .clip(RoundedCornerShape(8.dp))
//                                                        .background(Color(colorInt))
//                                                        .clickable {
//                                                            viewModel.applyFilterB(
//                                                                state.filterB.copy(
//                                                                    highlightsTint = colorInt
//                                                                )
//                                                            )
//                                                        }
//                                                )
//                                            }
//                                        }
//
//                                        Spacer(Modifier.height(8.dp))
//                                        Text("Shadows tint", color = Color.White)
//                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                            val presets = listOf(0x000000, 0x423F3E, 0x082F2E, 0x2B1B3D)
//                                            presets.forEach { colorInt ->
//                                                Box(
//                                                    modifier = Modifier
//                                                        .size(36.dp)
//                                                        .clip(RoundedCornerShape(8.dp))
//                                                        .background(Color(colorInt))
//                                                        .clickable {
//                                                            viewModel.applyFilterB(
//                                                                state.filterB.copy(
//                                                                    shadowsTint = colorInt
//                                                                )
//                                                            )
//                                                        }
//                                                )
//                                            }
//                                        }
//                                    }
//
//                                    "Categories" -> {
//                                        Column(
//                                            modifier = Modifier
//                                                .fillMaxWidth()
//                                                .height(300.dp)
//                                        ) {
//                                            LazyHorizontalGrid(
//                                                rows = GridCells.Adaptive(minSize = 128.dp),
//                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                                                verticalArrangement = Arrangement.spacedBy(8.dp),
//                                                modifier = Modifier.fillMaxWidth()
//                                            ) {
//                                                items(FILTER_PRESETS, key = { it.id }) { preset ->
//                                                    val drawablePreview = drawablePreviewCache[preset.id]
//                                                    val userThumb = presetPreviews[preset.id]
//                                                    val isSelected = false
//                                                    Box(
//                                                        modifier = Modifier
//                                                            .size(128.dp)
//                                                            .clip(RoundedCornerShape(8.dp))
//                                                            .background(if (isSelected) Primary else Secondary)
//                                                            .clickable {
//                                                                // Ensure we have a baseBitmap to process. If not, try to load from imageUri then apply preset.
//                                                                coroutineScope.launch {
//                                                                    val current = viewModel.state.value
//                                                                    if (current.baseBitmap == null && current.imageUri != null) {
//                                                                        try {
//                                                                            val loaded =
//                                                                                withContext(Dispatchers.IO) {
//                                                                                    ctx.contentResolver.openInputStream(
//                                                                                        current.imageUri
//                                                                                    )?.use { ins ->
//                                                                                        val opts =
//                                                                                            BitmapFactory.Options()
//                                                                                                .apply {
//                                                                                                    inPreferredConfig =
//                                                                                                        Bitmap.Config.ARGB_8888
//                                                                                                }
//                                                                                        val decoded =
//                                                                                            BitmapFactory.decodeStream(
//                                                                                                ins,
//                                                                                                null,
//                                                                                                opts
//                                                                                            )
//                                                                                                ?: return@use null
//                                                                                        // scale down to a reasonable size for editing
//                                                                                        val maxDim = 1200
//                                                                                        val scale =
//                                                                                            maxDim.toFloat() / kotlin.math.max(
//                                                                                                decoded.width,
//                                                                                                decoded.height
//                                                                                            )
//                                                                                        if (scale < 1f) Bitmap.createScaledBitmap(
//                                                                                            decoded,
//                                                                                            (decoded.width * scale).toInt(),
//                                                                                            (decoded.height * scale).toInt(),
//                                                                                            true
//                                                                                        ) else decoded
//                                                                                    }
//                                                                                }
//                                                                            if (loaded != null) {
//                                                                                viewModel.setBaseBitmap(
//                                                                                    loaded
//                                                                                )
//                                                                            }
//                                                                        } catch (e: Throwable) {
//                                                                            Log.w(
//                                                                                "EditorScreen",
//                                                                                "Failed loading baseBitmap for preset apply: ${e.message}"
//                                                                            )
//                                                                        }
//                                                                    }
//
//                                                                    // Now apply the preset filters (works whether base was already present or just set)
//                                                                    viewModel.applyFilter(
//                                                                        preset.filter
//                                                                            ?: viewModel.state.value.filter
//                                                                    )
//                                                                    viewModel.applyFilterA(
//                                                                        preset.filterA
//                                                                            ?: viewModel.state.value.filterA
//                                                                    )
//                                                                    viewModel.applyFilterB(
//                                                                        preset.filterB
//                                                                            ?: viewModel.state.value.filterB
//                                                                    )
//                                                                }
//                                                            },
//                                                        contentAlignment = Alignment.Center
//                                                    ) {
//                                                        if (drawablePreview != null) {
//                                                            Image(
//                                                                bitmap = drawablePreview,
//                                                                contentDescription = "preset preview",
//                                                                contentScale = ContentScale.Crop,
//                                                                modifier = Modifier.fillMaxSize()
//                                                            )
//                                                        } else if (userThumb != null) {
//                                                            Image(
//                                                                bitmap = userThumb,
//                                                                contentDescription = preset.displayName,
//                                                                contentScale = ContentScale.Crop,
//                                                                modifier = Modifier.fillMaxSize()
//                                                            )
//                                                        } else {
//                                                            Box(
//                                                                modifier = Modifier
//                                                                    .fillMaxSize()
//                                                                    .background(Color(0xFF1E293B))
//                                                            )
//                                                            if (isGeneratingPreviews.value) {
//                                                                Box(
//                                                                    modifier = Modifier.matchParentSize(),
//                                                                    contentAlignment = Alignment.Center
//                                                                ) {
//                                                                    CircularProgressIndicator(
//                                                                        color = Color.White,
//                                                                        strokeWidth = 2.dp
//                                                                    )
//                                                                }
//                                                            }
//                                                        }
//
//                                                        Text(
//                                                            text = preset.displayName,
//                                                            color = Color.White,
//                                                            style = MaterialTheme.typography.labelSmall,
//                                                            modifier = Modifier
//                                                                .align(Alignment.BottomCenter)
//                                                                .padding(4.dp)
//                                                        )
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    viewModel.applyFilterA(
                                        com.devsapiens.phonemagic.model.FilterAParams()
                                    )
                                    viewModel.applyFilterB(
                                        com.devsapiens.phonemagic.model.FilterBParams()
                                    )
                                }
                            ) { Text("Reset") }
                            Button(
                                onClick = {
                                    showFilters = false
                                }
                            ) { Text("Done") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            LazyHorizontalGrid(
                                rows = GridCells.Adaptive(minSize = 128.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(FILTER_PRESETS, key = { it.id }) { preset ->
                                    val drawablePreview =
                                        drawablePreviewCache[preset.id]
                                    val userThumb = presetPreviews[preset.id]
                                    val isSelected = false
                                    Box(
                                        modifier = Modifier
                                            .size(128.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Primary else Secondary)
                                            .clickable {
                                                // Ensure we have a baseBitmap to process. If not, try to load from imageUri then apply preset.
                                                coroutineScope.launch {
                                                    val current = viewModel.state.value
                                                    if (current.baseBitmap == null && current.imageUri != null) {
                                                        try {
                                                            val loaded =
                                                                withContext(Dispatchers.IO) {
                                                                    ctx.contentResolver.openInputStream(
                                                                        current.imageUri
                                                                    )?.use { ins ->
                                                                        val opts =
                                                                            BitmapFactory.Options()
                                                                                .apply {
                                                                                    inPreferredConfig =
                                                                                        Bitmap.Config.ARGB_8888
                                                                                }
                                                                        val decoded =
                                                                            BitmapFactory.decodeStream(
                                                                                ins,
                                                                                null,
                                                                                opts
                                                                            )
                                                                                ?: return@use null
                                                                        // scale down to a reasonable size for editing
                                                                        val maxDim =
                                                                            1200
                                                                        val scale =
                                                                            maxDim.toFloat() / kotlin.math.max(
                                                                                decoded.width,
                                                                                decoded.height
                                                                            )
                                                                        if (scale < 1f) Bitmap.createScaledBitmap(
                                                                            decoded,
                                                                            (decoded.width * scale).toInt(),
                                                                            (decoded.height * scale).toInt(),
                                                                            true
                                                                        ) else decoded
                                                                    }
                                                                }
                                                            if (loaded != null) {
                                                                viewModel.setBaseBitmap(
                                                                    loaded
                                                                )
                                                            }
                                                        } catch (e: Throwable) {
                                                            Log.w(
                                                                "EditorScreen",
                                                                "Failed loading baseBitmap for preset apply: ${e.message}"
                                                            )
                                                        }
                                                    }

                                                    // Now apply the preset filters (works whether base was already present or just set)
                                                    viewModel.applyFilter(
                                                        preset.filter
                                                            ?: viewModel.state.value.filter
                                                    )
                                                    viewModel.applyFilterA(
                                                        preset.filterA
                                                            ?: viewModel.state.value.filterA
                                                    )
                                                    viewModel.applyFilterB(
                                                        preset.filterB
                                                            ?: viewModel.state.value.filterB
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (drawablePreview != null) {
                                            Image(
                                                bitmap = drawablePreview,
                                                contentDescription = "preset preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (userThumb != null) {
                                            Image(
                                                bitmap = userThumb,
                                                contentDescription = preset.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF1E293B))
                                            )
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
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Row {
                            ButtonWithLabelComponent(
                                label = "filtro",
                                icon = R.drawable.ic_auto_awesome_motion_24,
                            ) {
                                showFilters = true
                            }
                        }

//                    Column(modifier = Modifier.padding(16.dp)) {
//                        Row(
//                            Modifier.fillMaxWidth(),
//                        ) {
//                            Button(
//                                onClick = {
//                                    showFilters = true; activeTab = "A"
//                                }
//                            ) { Text("Filtro A") }
//                            Spacer(Modifier.width(8.dp))
//                            Button(
//                                onClick = {
//                                    showFilters = true; activeTab = "B"
//                                }
//                            ) { Text("Filtro B") }
//                            Spacer(Modifier.width(8.dp))
//                            Button(
//                                onClick = {
//                                    showFilters = true; activeTab = "A"
//                                }
//                            ) { Text("Abrir Filtros") }
//                        }
//
//                        // Stickers / Text simple placeholders
//                        Row {
//                            androidx.compose.material3.Button(onClick = {
//                                viewModel.addLayer(
//                                    com.devsapiens.phonemagic.model.Layer.Sticker(
//                                        resId = android.R.drawable.star_on,
//                                        x = 50f,
//                                        y = 50f,
//                                        scale = 1f,
//                                        rotation = 0f
//                                    )
//                                )
//                            }) { Text("Agregar sticker") }
//                            Spacer(Modifier.width(8.dp))
//                            androidx.compose.material3.Button(onClick = {
//                                viewModel.addLayer(
//                                    com.devsapiens.phonemagic.model.Layer.Text(
//                                        text = "Hola",
//                                        color = 0xFF000000.toInt(),
//                                        sizeSp = 18f,
//                                        x = 100f,
//                                        y = 100f,
//                                        rotation = 0f
//                                    )
//                                )
//                            }) { Text("Agregar texto") }
//                        }
//                    }
                    }
                }
            }
        }
    }
}
