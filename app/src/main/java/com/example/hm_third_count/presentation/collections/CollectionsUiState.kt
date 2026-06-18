package com.example.hm_third_count.presentation.collections

data class CollectionSummary(
    val id: Long,
    val name: String,
    val colorHex: String,
    val countryCount: Int
)

data class CollectionsUiState(
    val isLoading: Boolean = true,
    val collections: List<CollectionSummary> = emptyList()
)

sealed class CollectionsEvent {
    data class Create(val name: String, val colorHex: String) : CollectionsEvent()
    data class Rename(val id: Long, val name: String) : CollectionsEvent()
    data class Delete(val id: Long) : CollectionsEvent()
}
