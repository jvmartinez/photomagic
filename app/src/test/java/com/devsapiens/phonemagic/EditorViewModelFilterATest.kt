package com.devsapiens.phonemagic

import com.devsapiens.phonemagic.model.FilterAParams
import com.devsapiens.phonemagic.viewmodel.EditorViewModel
import org.junit.Assert.*
import org.junit.Test

class EditorViewModelFilterATest {

    @Test
    fun applyFilterA_updatesStateAndSupportsUndoRedo() {
        val vm = EditorViewModel()
        val initial = vm.state.value

        // Apply Filter A
        val params = FilterAParams(intensity = 0.7f, warmth = 0.3f, vignette = 0.1f)
        vm.applyFilterA(params)
        val after = vm.state.value
        assertEquals(0.7f, after.filterA.intensity)
        assertEquals(0.3f, after.filterA.warmth)
        assertEquals(0.1f, after.filterA.vignette)

        // Undo
        vm.undo()
        val undone = vm.state.value
        assertEquals(initial.filterA.intensity, undone.filterA.intensity)
        assertEquals(initial.filterA.warmth, undone.filterA.warmth)
        assertEquals(initial.filterA.vignette, undone.filterA.vignette)

        // Redo
        vm.redo()
        val redone = vm.state.value
        assertEquals(after.filterA.intensity, redone.filterA.intensity)
        assertEquals(after.filterA.warmth, redone.filterA.warmth)
        assertEquals(after.filterA.vignette, redone.filterA.vignette)
    }

    @Test
    fun applyFilterA_clampsOutOfRangeValues() {
        val vm = EditorViewModel()

        val params = FilterAParams(intensity = 2.0f, warmth = -2.0f, vignette = -1.0f)
        vm.applyFilterA(params)
        val after = vm.state.value

        assertEquals(1.0f, after.filterA.intensity)
        assertEquals(-1.0f, after.filterA.warmth)
        assertEquals(0.0f, after.filterA.vignette)
    }
}

