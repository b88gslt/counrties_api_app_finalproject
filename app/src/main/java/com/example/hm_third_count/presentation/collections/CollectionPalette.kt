package com.example.hm_third_count.presentation.collections

import androidx.compose.ui.graphics.Color

/** Преднабор цветов для коллекций — пользователь выбирает один при создании. */
internal val CollectionColors: List<String> = listOf(
    "#EF5350", // red
    "#FF9800", // orange
    "#FFEB3B", // yellow
    "#66BB6A", // green
    "#26C6DA", // cyan
    "#4FC3F7", // light blue
    "#5C6BC0", // indigo
    "#AB47BC"  // purple
)

internal fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)
