package com.example.hm_third_count.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    COUNTRIES(route = "countries_tab", label = "Countries", icon = Icons.Filled.Public),
    JOURNAL(route = "journal_tab", label = "Journal", icon = Icons.Filled.TravelExplore),
    COLLECTIONS(route = "collections_tab", label = "Collections", icon = Icons.Filled.Bookmark),
    SETTINGS(route = "settings_tab", label = "Settings", icon = Icons.Filled.Settings);

}
