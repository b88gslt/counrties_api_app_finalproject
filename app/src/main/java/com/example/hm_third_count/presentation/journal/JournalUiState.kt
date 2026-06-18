package com.example.hm_third_count.presentation.journal

import com.example.hm_third_count.data.model.Country

data class VisitedItem(
    val country: Country,
    val visitedAt: Long,
    val rating: Int,
    val note: String?
)

data class WishlistItem(
    val country: Country,
    val priority: Int,
    val plannedDate: Long?
)

data class NoteItem(
    val country: Country,
    val text: String,
    val updatedAt: Long
)

data class JournalStats(
    val visitedCount: Int = 0,
    val regionsCount: Int = 0,
    val averageRating: Float = 0f,
    val wishlistCount: Int = 0,
    val notesCount: Int = 0
)

data class JournalUiState(
    val isLoading: Boolean = true,
    val visited: List<VisitedItem> = emptyList(),
    val wishlist: List<WishlistItem> = emptyList(),
    val notes: List<NoteItem> = emptyList(),
    val stats: JournalStats = JournalStats()
)

sealed class JournalEvent {
    data class RemoveVisit(val countryCode: String) : JournalEvent()
    data class RemoveWishlist(val countryCode: String) : JournalEvent()
    data class RemoveNote(val countryCode: String) : JournalEvent()
}
