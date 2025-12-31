package com.devsapiens.phonemagic

import com.devsapiens.phonemagic.model.FilterParams
import com.devsapiens.phonemagic.viewmodel.EditorViewModel
import org.junit.Assert.*
import org.junit.Test

class EditorViewModelTest {

    @Test
    fun undoRedo_workflow() {
        val vm = EditorViewModel()
        val initial = vm.state.value
        // Apply a filter
        vm.applyFilter(FilterParams(brightness = 0.2f))
        val after = vm.state.value
        assertNotEquals(initial.filter.brightness, after.filter.brightness)

        // Undo
        vm.undo()
        val undone = vm.state.value
        assertEquals(initial.filter.brightness, undone.filter.brightness)

        // Redo
        vm.redo()
        val redone = vm.state.value
        assertEquals(after.filter.brightness, redone.filter.brightness)
    }
}

