package com.gk.kwikpass.utils

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonObject

fun Modifier.applyStyles(wrapper: ModifierWrapper?): Modifier {
    if (wrapper?.modifierData.isNullOrEmpty()) return this

    var modifier = this
    try {
        val gson = Gson()
        val styleMap = gson.fromJson(wrapper?.modifierData, Map::class.java) as? Map<String, Any>

        styleMap?.entries?.forEach { (key, value) ->
            when (key) {
                "padding" -> {
                    when (value) {
                        is Number -> {
                            modifier = modifier.padding(value.toString().toFloat().dp)
                        }
                        is Map<*, *> -> {
                            val top = (value["top"] as? Double)?.toFloat() ?: 0f
                            val bottom = (value["bottom"] as? Double)?.toFloat() ?: 0f
                            val start = (value["start"] as? Double)?.toFloat() ?: 0f
                            val end = (value["end"] as? Double)?.toFloat() ?: 0f
                            modifier = modifier.padding(
                                start = start.dp,
                                top = top.dp,
                                end = end.dp,
                                bottom = bottom.dp
                            )
                        }
                    }
                }
                "height" -> {
                    when (value) {
                        is Number -> modifier = modifier.height(value.toString().toFloat().dp)
                        is String -> if (value == "fill") modifier = modifier.fillMaxHeight()
                    }
                }
                "width" -> {
                    when (value) {
                        is Number -> modifier = modifier.width(value.toString().toFloat().dp)
                        is String -> if (value == "fill") modifier = modifier.fillMaxWidth()
                    }
                }
                // Add more style properties as needed
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return modifier
}
