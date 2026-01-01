package com.devsapiens.phonemagic.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsapiens.phonemagic.model.EditorState
import com.devsapiens.phonemagic.model.FilterParams
import com.devsapiens.phonemagic.model.FilterAParams
import com.devsapiens.phonemagic.model.FilterBParams
import com.devsapiens.phonemagic.model.EnhanceParams
import com.devsapiens.phonemagic.model.Layer
import com.devsapiens.phonemagic.processor.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<EditorState>()
    private val redoStack = ArrayDeque<EditorState>()

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
    fun applyFilterA(params: FilterAParams) = commit { it.copy(filterA = clampFilterA(params)) }

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
        return ImageProcessor.processAll(_state.value)
    }

    /**
     * Returns a new Bitmap with all current filters/layers applied (the 'baked' image).
     * This is CPU-intensive and should be called from a background dispatcher.
     */
    fun bakeFilters(): Bitmap? {
        return ImageProcessor.processAll(_state.value)
    }

    /**
     * Apply a baked bitmap into the state using commit so the action is undoable.
     */
    fun applyBakedBitmap(bitmap: Bitmap) = commit { it.copy(baseBitmap = bitmap) }
}
