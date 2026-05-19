package com.example.hm_third_count.presentation.recent

import com.example.hm_third_count.data.model.Country

data class RecentItem(
    val country: Country,
    val viewedAt: Long
)

data class RecentUiState(
    val isLoading: Boolean = true,
    val items: List<RecentItem> = emptyList()
)

sealed class RecentEvent {
    data object ClearAll : RecentEvent()
    data class Remove(val countryCode: String) : RecentEvent()
}
