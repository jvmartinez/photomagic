package com.devsapiens.phonemagic.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsapiens.phonemagic.component.slider.SliderEnum
import com.devsapiens.phonemagic.model.EditorState
import com.devsapiens.phonemagic.model.FilterParams
import com.devsapiens.phonemagic.model.FilterAParams
import com.devsapiens.phonemagic.model.FilterBParams
import com.devsapiens.phonemagic.model.EnhanceParams
import com.devsapiens.phonemagic.model.Layer
import com.devsapiens.phonemagic.processor.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<EditorState>()
    private val redoStack = ArrayDeque<EditorState>()

    // Save flow
    sealed class SaveStatus {
        object Idle : SaveStatus()
        object Saving : SaveStatus()
        data class Success(val uri: Uri) : SaveStatus()
        data class Error(val message: String) : SaveStatus()
    }

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _selectedSetting = MutableStateFlow<SliderEnum>(SliderEnum.None)
    val selectedSetting: StateFlow<SliderEnum> = _selectedSetting

    private val _isCropping = MutableStateFlow(false)
    val isCropping: StateFlow<Boolean> = _isCropping.asStateFlow()

    fun setSelectedSetting(setting: SliderEnum) {
        _selectedSetting.value = setting
    }

    fun clearState() {
        _state.value = EditorState()
        clearHistory()
    }

    fun setImageUri(uri: Uri?) {
        updateState { it.copy(imageUri = uri) }
        clearHistory()
    }

    fun setBaseBitmap(bitmap: Bitmap?) {
        updateState { it.copy(baseBitmap = bitmap) }
        clearHistory()
    }

    fun applyFilter(params: FilterParams) = commit { it.copy(filter = params) }
    fun applyEnhance(params: EnhanceParams) = commit { it.copy(enhance = params) }

    // New: apply Filter A params with clamping
    fun applyFilterA(params: FilterAParams) = commit {
        it.copy(filterA = clampFilterA(params))
    }

    // New: apply Filter B params with clamping
    fun applyFilterB(params: FilterBParams) = commit { it.copy(filterB = clampFilterB(params)) }

    fun addLayer(layer: Layer) = commit { it.copy(layers = it.layers + layer) }
    fun updateLayers(layers: List<Layer>) = commit { it.copy(layers = layers) }
    fun clearLayers() = commit { it.copy(layers = emptyList()) }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeLast()
            redoStack.addLast(_state.value)
            _state.value = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(_state.value)
            _state.value = next
        }
    }

    fun reset() {
        updateState { EditorState(imageUri = it.imageUri, baseBitmap = it.baseBitmap) }
        clearHistory()
    }

    private fun clearHistory() {
        undoStack.clear(); redoStack.clear()
    }

    private fun commit(transform: (EditorState) -> EditorState) {
        val current = _state.value
        val newState = transform(current)
        undoStack.addLast(current)
        redoStack.clear()
        _state.value = newState
    }

    private fun updateState(transform: (EditorState) -> EditorState) {
        _state.value = transform(_state.value)
    }

    fun requestUpscale() {
        // Placeholder for AI upscale async task
        viewModelScope.launch {
            val current = _state.value
            _state.value = current.copy(enhance = current.enhance.copy(upscaleRequested = true))
        }
    }

    // Helper: clamp FilterA param ranges
    private fun clampFilterA(params: FilterAParams): FilterAParams {
        return params.copy(
            intensity = params.intensity.coerceIn(0f, 1f),
            warmth = params.warmth.coerceIn(-1f, 1f),
            vignette = params.vignette.coerceIn(0f, 1f)
        )
    }

    // Helper: clamp FilterB params
    private fun clampFilterB(params: FilterBParams): FilterBParams {
        return params.copy(
            highlightsTint = params.highlightsTint and 0xFFFFFF,
            shadowsTint = params.shadowsTint and 0xFFFFFF,
            strength = params.strength.coerceIn(0f, 1f)
        )
    }

    // Export: render baseBitmap with current state filters using ImageProcessor
    fun exportBitmap(): Bitmap? {
        return ImageProcessor.processAll(_state.value, highQuality = true)
    }

    /**
     * Returns a new Bitmap with all current filters/layers applied (the 'baked' image).
     * This is CPU-intensive and should be called from a background dispatcher.
     */
    fun bakeFilters(): Bitmap? {
        return ImageProcessor.processAll(_state.value, highQuality = true)
    }

    /**
     * Apply a baked bitmap into the state using commit so the action is undoable.
     */
    fun applyBakedBitmap(bitmap: Bitmap) = commit { it.copy(baseBitmap = bitmap) }

    /**
     * Public: save the currently exported/baked image to device storage (Pictures/PhoneMagic).
     * Emits progress/results to [saveStatus] StateFlow.
     * This will handle API Q+ via MediaStore RELATIVE_PATH and fall back to legacy file write for older devices.
     */
    fun saveEditedImage(context: Context, filename: String? = null) {
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            try {
                val bitmap = withContext(Dispatchers.Default) { exportBitmap() }
                if (bitmap == null) {
                    _saveStatus.value = SaveStatus.Error("No image to save")
                    return@launch
                }
                val uri = withContext(Dispatchers.IO) {
                    saveBitmapToMediaStore(context, bitmap, filename)
                }
                if (uri != null) {
                    _saveStatus.value = SaveStatus.Success(uri)
                } else {
                    _saveStatus.value = SaveStatus.Error("Failed to save image")
                }
            } catch (t: Throwable) {
                _saveStatus.value = SaveStatus.Error(t.message ?: "Unknown error")
            }
        }
    }

    // Helper that actually writes the Bitmap to storage and returns the resulting Uri (or null)
    private suspend fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, filename: String? = null): Uri? {
        return withContext(Dispatchers.IO) {
            val displayName = filename ?: "phonemagic_${System.currentTimeMillis()}.png"
            val mimeType = "image/png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "PhoneMagic")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
                var out: OutputStream? = null
                try {
                    out = resolver.openOutputStream(uri)
                    out?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                } catch (e: Exception) {
                    // If write failed, try to cleanup
                    try { uri.let { resolver.delete(it, null, null) } } catch (_: Exception) {}
                    return@withContext null
                } finally {
                    // mark not pending
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    try { resolver.update(uri, values, null, null) } catch (_: Exception) {}
                }
                return@withContext uri
            } else {
                // Legacy path: write to Pictures/PhoneMagic (requires WRITE_EXTERNAL_STORAGE on older devices)
                try {
                    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val dir = File(pictures, "PhoneMagic")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, displayName)
                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                    }
                    // Return file:// URI; caller can trigger MediaScanner if needed
                    return@withContext Uri.fromFile(file)
                } catch (e: Exception) {
                    return@withContext null
                }
            }
        }
    }

    fun resizeEditedImage() {
        // Enter cropping mode in the UI
        _isCropping.value = true
    }

    fun cancelCrop() {
        _isCropping.value = false
    }

    /**
     * Apply a crop using normalized coordinates in [0..1] relative to the current base bitmap.
     * left/top/right/bottom are normalized fractions.
     */
    fun applyCropNormalized(left: Float, top: Float, right: Float, bottom: Float) {
        val base = _state.value.baseBitmap ?: return
        // clamp inputs
        val l = left.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val r = right.coerceIn(0f, 1f)
        val b = bottom.coerceIn(0f, 1f)
        if (r <= l || b <= t) {
            // invalid rect -> cancel
            _isCropping.value = false
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val width = base.width
                val height = base.height
                val x = (l * width).toInt().coerceIn(0, width - 1)
                val y = (t * height).toInt().coerceIn(0, height - 1)
                val w = ((r - l) * width).toInt().coerceAtLeast(1).coerceIn(1, width - x)
                val h = ((b - t) * height).toInt().coerceAtLeast(1).coerceIn(1, height - y)
                val cropped = Bitmap.createBitmap(base, x, y, w, h)
                // commit as a new state so it's undoable
                commit { it.copy(baseBitmap = cropped) }
            } catch (_: Throwable) {
                // ignore crop failure
            } finally {
                _isCropping.value = false
            }
        }
    }
}
