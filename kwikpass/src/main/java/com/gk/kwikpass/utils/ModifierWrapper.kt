package com.gk.kwikpass.utils

import androidx.compose.ui.Modifier

data class ModifierWrapper(
    val modifierData: String // JSON string representing modifier properties
) {
    companion object {
        fun empty() = ModifierWrapper("")
    }
}
