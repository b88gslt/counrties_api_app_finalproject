package com.example.hm_third_count.presentation.detail

import com.example.hm_third_count.data.local.AreaUnit
import com.example.hm_third_count.data.model.Country

data class CountryDetailUiState(
    val isLoading: Boolean = false,
    val country: Country? = null,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val fetchedAt: Long? = null,
    val isStale: Boolean = false,
    val isRefreshing: Boolean = false,
    // ----- Travel Journal -----
    /** Если страна посещена — заполнено. */
    val visit: VisitView? = null,
    /** Если в wishlist — заполнено. */
    val wishlist: WishlistView? = null,
    /** Заметка пользователя, если есть. */
    val note: String? = null,
    // ----- Collections -----
    /** Все коллекции активного профиля (для multi-select-диалога). */
    val allCollections: List<CollectionSummaryView> = emptyList(),
    /** В каких коллекциях сейчас находится эта страна. */
    val memberOfCollections: Set<Long> = emptySet(),
    /** Единица измерения площади из настроек. */
    val areaUnit: AreaUnit = AreaUnit.KM2
)

data class CollectionSummaryView(
    val id: Long,
    val name: String,
    val colorHex: String
)

data class VisitView(
    val visitedAt: Long,
    val rating: Int,
    val note: String?
)

data class WishlistView(
    val priority: Int,
    val plannedDate: Long?
)

sealed class CountryDetailEvent {
    data object Retry : CountryDetailEvent()
    data object ToggleFavorite : CountryDetailEvent()
    data object ManualRefresh : CountryDetailEvent()
    data class SaveVisit(val visitedAt: Long, val rating: Int, val note: String?) : CountryDetailEvent()
    data object DeleteVisit : CountryDetailEvent()
    data class SaveWishlist(val priority: Int, val plannedDate: Long?) : CountryDetailEvent()
    data object DeleteWishlist : CountryDetailEvent()
    data class SaveNote(val text: String) : CountryDetailEvent()
    data object DeleteNote : CountryDetailEvent()
    data class SetCollections(val selectedIds: Set<Long>) : CountryDetailEvent()
    data class CreateAndAddCollection(val name: String, val colorHex: String) : CountryDetailEvent()
}
