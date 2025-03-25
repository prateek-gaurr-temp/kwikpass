package com.gk.kwikpass.utils

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object StyleParser {
    fun parseStyle(styleMap: Map<String, Any>?): Modifier {
        if (styleMap == null) return Modifier

        var modifier = Modifier

        styleMap.forEach { (key, value) ->
            when (key) {
                "padding" -> {
                    modifier = when (value) {
                        is Number -> modifier.padding(value.toFloat().dp) as Modifier.Companion
                        is Map<*, *> -> ({
                            modifier.padding(
                                start = (value["start"] as? Number)?.toFloat()?.dp ?: 0.dp,
                                top = (value["top"] as? Number)?.toFloat()?.dp ?: 0.dp,
                                end = (value["end"] as? Number)?.toFloat()?.dp ?: 0.dp,
                                bottom = (value["bottom"] as? Number)?.toFloat()?.dp ?: 0.dp
                            )
                        }) as Modifier.Companion
                        else -> modifier
                    }
                }
                "width" -> {
                    modifier = when (value) {
                        "fill" -> modifier.fillMaxWidth() as Modifier.Companion
                        is Number -> modifier.width(value.toFloat().dp) as Modifier.Companion
                        else -> modifier
                    }
                }
                "height" -> {
                    modifier = when (value) {
                        "fill" -> modifier.fillMaxHeight() as Modifier.Companion
                        is Number -> modifier.height(value.toFloat().dp) as Modifier.Companion
                        else -> modifier
                    }
                }
            }
        }

        return modifier
    }

    fun parseModifierStyle(style: Map<String, Any>?): Modifier.Companion {
        return Modifier as Modifier.Companion
    }
}
