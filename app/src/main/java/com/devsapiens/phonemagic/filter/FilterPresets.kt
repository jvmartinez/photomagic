package com.devsapiens.phonemagic.filter

import com.devsapiens.phonemagic.R
import com.devsapiens.phonemagic.model.FilterAParams
import com.devsapiens.phonemagic.model.FilterBParams
import com.devsapiens.phonemagic.model.FilterParams

data class FilterPreset(
    val id: String,
    val displayName: String,
    val filter: FilterParams? = null,
    val filterA: FilterAParams? = null,
    val filterB: FilterBParams? = null,
    // Resource id for a custom preview image (defaults to bundled ic_preview_filter)
    val previewRes: Int = R.drawable.ic_preview_filter
)

// Presets definitions — approximate parameters for visual effect
val FILTER_PRESETS = listOf(
    FilterPreset(
        id = "uv",
        displayName = "Filtro UV",
        filter = FilterParams(brightness = -0.05f, contrast = 0.95f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.15f, warmth = -0.15f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "nd_grad",
        displayName = "ND Degradado",
        filter = FilterParams(brightness = -0.2f, contrast = 0.95f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.1f, warmth = 0f, vignette = 0.1f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "grad_soft",
        displayName = "Grad. Suave",
        filter = FilterParams(brightness = 0f, contrast = 0.95f, saturation = 0.95f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.05f, vignette = 0.08f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "grad_hard",
        displayName = "Grad. Fuerte",
        filter = FilterParams(brightness = -0.1f, contrast = 0.9f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.3f, warmth = 0.0f, vignette = 0.2f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "blender",
        displayName = "Grad. Blender",
        filter = FilterParams(brightness = 0f, contrast = 0.9f, saturation = 1.05f),
        filterB = FilterBParams(highlightsTint = 0xFFE8DCCF.toInt(), shadowsTint = 0x101826, strength = 0.18f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "night",
        displayName = "Fotografía Nocturna",
        filter = FilterParams(brightness = 0.15f, contrast = 0.9f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.05f, warmth = -0.2f, vignette = 0.25f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "macro",
        displayName = "Filtro Macro",
        filter = FilterParams(brightness = 0f, contrast = 1.15f, saturation = 1.2f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.05f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "closeup",
        displayName = "Filtro Close-up",
        filter = FilterParams(brightness = 0.03f, contrast = 1.05f, saturation = 1.1f),
        filterA = FilterAParams(intensity = 0.08f, warmth = 0.08f, vignette = 0.03f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "infrared",
        displayName = "Filtro Infrarrojo",
        filter = FilterParams(brightness = 0.1f, contrast = 0.95f, saturation = 0.2f),
        filterA = FilterAParams(intensity = 0.5f, warmth = 0.8f, vignette = 0.15f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "skylight",
        displayName = "Filtro Skylight",
        filter = FilterParams(brightness = 0.05f, contrast = 1.0f, saturation = 1.05f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.15f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "enhancer",
        displayName = "Filtro Enhancer",
        filter = FilterParams(brightness = 0.02f, contrast = 1.1f, saturation = 1.2f),
        filterA = FilterAParams(intensity = 0.15f, warmth = 0.05f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "star",
        displayName = "Filtro Estrella",
        filter = FilterParams(brightness = 0.05f, contrast = 1.0f, saturation = 1.05f),
        filterB = FilterBParams(highlightsTint = 0xFFFCE9C7.toInt(), shadowsTint = 0x000000, strength = 0.25f),
        previewRes = R.drawable.ic_preview_filter
    ),
    FilterPreset(
        id = "fog",
        displayName = "Filtro Niebla",
        filter = FilterParams(brightness = 0.12f, contrast = 0.85f, saturation = 0.85f),
        filterA = FilterAParams(intensity = 0.05f, warmth = -0.05f, vignette = 0.06f),
        previewRes = R.drawable.ic_preview_filter
    ),
    // New: Blanco y Negro preset
    FilterPreset(
        id = "bw",
        displayName = "Blanco y Negro",
        filter = FilterParams(brightness = 0f, contrast = 1.05f, saturation = 0f),
        filterA = FilterAParams(intensity = 0f, warmth = 0f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter
    ),
    // New: Beautify preset
    FilterPreset(
        id = "beauty",
        displayName = "Embellecer",
        filter = FilterParams(brightness = 0.01f, contrast = 1.02f, saturation = 1.03f),
        filterA = FilterAParams(intensity = 0.6f, warmth = 0.06f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter
    )
)
