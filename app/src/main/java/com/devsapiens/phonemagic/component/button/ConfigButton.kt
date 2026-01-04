package com.devsapiens.phonemagic.component.button

import androidx.compose.ui.graphics.Color


data class ConfigButton<T>(
    val title: String,
    val icon: Int? = null,
    val bgColor: Color? = null,
    val textColor: Color? = null,
    val borderColor: Color? = null,
    val borderWidth: Float = 0f,
    val onClick: (T) -> Unit
)

