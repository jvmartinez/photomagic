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
    val previewRes: Int = R.drawable.ic_preview_filter_2
)

// Presets definitions — approximate parameters for visual effect
val FILTER_PRESETS = listOf(
    FilterPreset(
        id = "uv",
        displayName = "Filtro UV",
        filter = FilterParams(brightness = -0.05f, contrast = 0.95f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.15f, warmth = -0.15f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "nd_grad",
        displayName = "ND Degradado",
        filter = FilterParams(brightness = -0.2f, contrast = 0.95f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.1f, warmth = 0f, vignette = 0.1f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "grad_soft",
        displayName = "Grad. Suave",
        filter = FilterParams(brightness = 0f, contrast = 0.95f, saturation = 0.95f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.05f, vignette = 0.08f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "grad_hard",
        displayName = "Grad. Fuerte",
        filter = FilterParams(brightness = -0.1f, contrast = 0.9f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.3f, warmth = 0.0f, vignette = 0.2f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "blender",
        displayName = "Grad. Blender",
        filter = FilterParams(brightness = 0f, contrast = 0.9f, saturation = 1.05f),
        filterB = FilterBParams(highlightsTint = 0xFFE8DCCF.toInt(), shadowsTint = 0x101826, strength = 0.18f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "night",
        displayName = "Fotografía Nocturna",
        filter = FilterParams(brightness = 0.15f, contrast = 0.9f, saturation = 0.9f),
        filterA = FilterAParams(intensity = 0.05f, warmth = -0.2f, vignette = 0.25f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "macro",
        displayName = "Filtro Macro",
        filter = FilterParams(brightness = 0f, contrast = 1.15f, saturation = 1.2f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.05f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "closeup",
        displayName = "Filtro Close-up",
        filter = FilterParams(brightness = 0.03f, contrast = 1.05f, saturation = 1.1f),
        filterA = FilterAParams(intensity = 0.08f, warmth = 0.08f, vignette = 0.03f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "infrared",
        displayName = "Filtro Infrarrojo",
        filter = FilterParams(brightness = 0.1f, contrast = 0.95f, saturation = 0.2f),
        filterA = FilterAParams(intensity = 0.5f, warmth = 0.8f, vignette = 0.15f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "skylight",
        displayName = "Filtro Skylight",
        filter = FilterParams(brightness = 0.05f, contrast = 1.0f, saturation = 1.05f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.15f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "enhancer",
        displayName = "Filtro Enhancer",
        filter = FilterParams(brightness = 0.02f, contrast = 1.1f, saturation = 1.2f),
        filterA = FilterAParams(intensity = 0.15f, warmth = 0.05f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "star",
        displayName = "Filtro Estrella",
        filter = FilterParams(brightness = 0.05f, contrast = 1.0f, saturation = 1.05f),
        filterB = FilterBParams(highlightsTint = 0xFFFCE9C7.toInt(), shadowsTint = 0x000000, strength = 0.25f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "fog",
        displayName = "Filtro Niebla",
        filter = FilterParams(brightness = 0.12f, contrast = 0.85f, saturation = 0.85f),
        filterA = FilterAParams(intensity = 0.05f, warmth = -0.05f, vignette = 0.06f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    // Stylized presets
    FilterPreset(
        id = "retro",
        displayName = "Retro",
        filter = FilterParams(brightness = 0f, contrast = 0.95f, saturation = 0.9f, stylize = "retro", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.05f, vignette = 0.06f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "vhs",
        displayName = "VHS",
        filter = FilterParams(brightness = 0f, contrast = 1.02f, saturation = 1.05f, stylize = "vhs", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.08f, warmth = -0.05f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "polaroid",
        displayName = "Polaroid",
        filter = FilterParams(brightness = 0.02f, contrast = 1.02f, saturation = 1.08f, stylize = "polaroid", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.06f, warmth = 0.08f, vignette = 0.04f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "year90",
        displayName = "Años 90",
        filter = FilterParams(brightness = 0f, contrast = 1.15f, saturation = 1.2f, stylize = "year90", stylizeIntensity = 0.95f),
        filterA = FilterAParams(intensity = 0.1f, warmth = 0.02f, vignette = 0.03f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "grain",
        displayName = "Granulado",
        filter = FilterParams(brightness = 0f, contrast = 1.0f, saturation = 1.0f, stylize = "grain", stylizeIntensity = 0.6f),
        filterA = FilterAParams(intensity = 0.04f, warmth = 0f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "film",
        displayName = "Película",
        filter = FilterParams(brightness = 0f, contrast = 1.05f, saturation = 1.02f, stylize = "film", stylizeIntensity = 0.85f),
        filterA = FilterAParams(intensity = 0.08f, warmth = 0.06f, vignette = 0.07f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    // Blur preset
    FilterPreset(
        id = "blur",
        displayName = "Desenfoque",
        filter = FilterParams(brightness = 0f, contrast = 1.0f, saturation = 1.0f, stylize = "blur", stylizeIntensity = 0.75f),
        filterA = FilterAParams(intensity = 0f, warmth = 0f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    // New: Blanco y Negro preset
    FilterPreset(
        id = "bw",
        displayName = "Blanco y Negro",
        filter = FilterParams(brightness = 0f, contrast = 1.05f, saturation = 0f),
        filterA = FilterAParams(intensity = 0f, warmth = 0f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    // New: Beautify preset
    FilterPreset(
        id = "beauty",
        displayName = "Embellecer",
        filter = FilterParams(brightness = 0.01f, contrast = 1.02f, saturation = 1.03f),
        filterA = FilterAParams(intensity = 0.6f, warmth = 0.06f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    // New custom stylized presets requested
    FilterPreset(
        id = "pelicula_instantanea",
        displayName = "Película Instantánea",
        filter = FilterParams(brightness = 0.03f, contrast = 1.05f, saturation = 1.1f, stylize = "instant", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.12f, warmth = 0.08f, vignette = 0.06f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "juventas",
        displayName = "Juventas",
        filter = FilterParams(brightness = 0.04f, contrast = 1.08f, saturation = 1.15f, stylize = "juventas", stylizeIntensity = 0.85f),
        filterA = FilterAParams(intensity = 0.15f, warmth = 0.1f, vignette = 0.04f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "vesper",
        displayName = "Vesper",
        filter = FilterParams(brightness = -0.02f, contrast = 1.02f, saturation = 1.05f, stylize = "vesper", stylizeIntensity = 0.8f),
        filterA = FilterAParams(intensity = 0.09f, warmth = -0.02f, vignette = 0.07f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "periwinkle",
        displayName = "Periwinkle",
        filter = FilterParams(brightness = 0f, contrast = 1.0f, saturation = 1.08f, stylize = "periwinkle", stylizeIntensity = 0.7f),
        filterB = FilterBParams(highlightsTint = 0xFFDDE8FF.toInt(), shadowsTint = 0x2A1E4F, strength = 0.25f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "clasico",
        displayName = "Clásico",
        filter = FilterParams(brightness = 0f, contrast = 1.08f, saturation = 0.95f, stylize = "classic", stylizeIntensity = 0.85f),
        filterA = FilterAParams(intensity = 0.07f, warmth = 0.04f, vignette = 0.05f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "perla",
        displayName = "Perla",
        filter = FilterParams(brightness = 0.06f, contrast = 1.0f, saturation = 0.9f, stylize = "pearl", stylizeIntensity = 0.6f),
        filterA = FilterAParams(intensity = 0.05f, warmth = -0.01f, vignette = 0.03f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "calabaza",
        displayName = "Calabaza",
        filter = FilterParams(brightness = 0.02f, contrast = 1.03f, saturation = 1.15f, stylize = "pumpkin", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.14f, warmth = 0.25f, vignette = 0.06f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "hielo_negro",
        displayName = "Hielo Negro",
        filter = FilterParams(brightness = -0.05f, contrast = 0.95f, saturation = 0.6f, stylize = "ice_black", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.06f, warmth = -0.3f, vignette = 0.18f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "ciberpunk",
        displayName = "Ciberpunk",
        filter = FilterParams(brightness = 0f, contrast = 1.05f, saturation = 1.25f, stylize = "cyberpunk", stylizeIntensity = 0.95f),
        filterB = FilterBParams(highlightsTint = 0xFF00FFC8.toInt(), shadowsTint = 0xFF2B00FF.toInt(), strength = 0.35f),
        previewRes = R.drawable.ic_preview_filter_2
    ),
    FilterPreset(
        id = "fantasia",
        displayName = "Fantasía",
        filter = FilterParams(brightness = 0.04f, contrast = 1.02f, saturation = 1.3f, stylize = "fantasy", stylizeIntensity = 0.9f),
        filterA = FilterAParams(intensity = 0.18f, warmth = 0.06f, vignette = 0.02f),
        previewRes = R.drawable.ic_preview_filter_2
    )
)
