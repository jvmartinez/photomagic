package com.devsapiens.phonemagic.component.slider

import com.devsapiens.phonemagic.R

enum class SliderEnum(val iconRes: Int, val displayName: String, val defaultValue: Float) {
    Explosion(R.drawable.ic_exposure_24, "exposición", 0f),    // exposición
    Brightness(R.drawable.ic_brightness_6_24, "brillo", 0f),   // brillo
    Constant(R.drawable.ic_contrast_24, "contraste", 1f),     // contraste (valor neutro 1)
    Clarity(R.drawable.ic_light_mode_24, "claridad", 0f),      // claridad
    Saturation(R.drawable.ic_filter_b_and_w_24, "saturación", 1f),   // saturación (neutro 1)
    Vibration(R.drawable.ic_photo_size_select_large_24, "vibración", 0f),    // vibración
    Warmth(R.drawable.ic_brush_24, "calidez", 0f),       // calidez
    Shadows(R.drawable.ic_exposure_24, "sombras", 0f),      // sombras
    Dispersion(R.drawable.ic_shadow_24, "dispersion", 0f),   // dispersion / dehaze
    Grain(R.drawable.ic_grain_24, "grano", 0f),        // grano
    None(R.drawable.ic_cancel_24, "cerrar", 0f);        // sin uso / placeholder

    companion object {
        /** Devuelve un Array con todos los valores del enum. */
        fun toArray(): Array<SliderEnum> = entries.toTypedArray()

        /** Devuelve un IntArray con los códigos de cada enum en el mismo orden que [toArray()]. */
        fun codes(): IntArray = entries.map { it.iconRes }.toIntArray()

        /** Devuelve un FloatArray con los valores por defecto de cada enum en el mismo orden que [toArray()]. */
        fun defaults(): FloatArray = entries.map { it.defaultValue }.toFloatArray()

        /** Obtiene el enum a partir de su código entero; devuelve null si no existe. */
        fun fromCode(code: Int): SliderEnum? = entries.find { it.iconRes == code }

        /** Obtiene el valor por defecto para un código, o null si no hay ese código. */
        fun defaultForCode(code: Int): Float? = fromCode(code)?.defaultValue
    }
}
