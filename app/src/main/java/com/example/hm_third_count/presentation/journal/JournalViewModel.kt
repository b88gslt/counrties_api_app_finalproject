package com.example.hm_third_count.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.CountryCacheEntity
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val countryCacheDao: CountryCacheDao,
    private val json: Json
) : ViewModel() {

    /**
     * Реактивная сборка экрана Journal из 4 источников Room (через JournalRepository):
     *  - visits + wishlist + notes + country_cache.
     * При смене активного профиля все три потока перестраиваются (flatMapLatest внутри JournalRepository).
     */
    val uiState: StateFlow<JournalUiState> = combine(
        journalRepository.visits,
        journalRepository.wishlist,
        journalRepository.notes,
        countryCacheDao.observeAll()
    ) { visits, wishlist, notes, cache ->
        val cacheByCode = cache.associateBy { it.countryCode }
        val visitedItems = visits.mapNotNull { v ->
            val country = decode(cacheByCode[v.countryCode]) ?: return@mapNotNull null
            VisitedItem(country, v.visitedAt, v.rating, v.note)
        }
        val wishlistItems = wishlist.mapNotNull { w ->
            val country = decode(cacheByCode[w.countryCode]) ?: return@mapNotNull null
            WishlistItem(country, w.priority, w.plannedDate)
        }
        val noteItems = notes.mapNotNull { n ->
            val country = decode(cacheByCode[n.countryCode]) ?: return@mapNotNull null
            NoteItem(country, n.text, n.updatedAt)
        }
        JournalUiState(
            isLoading = false,
            visited = visitedItems,
            wishlist = wishlistItems,
            notes = noteItems,
            stats = computeStats(visitedItems, wishlistItems, noteItems)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        JournalUiState(isLoading = true)
    )

    private fun decode(entity: CountryCacheEntity?): Country? {
        if (entity == null) return null
        return runCatching { json.decodeFromString<Country>(entity.json) }.getOrNull()
    }

    private fun computeStats(
        visited: List<VisitedItem>,
        wishlist: List<WishlistItem>,
        notes: List<NoteItem>
    ): JournalStats {
        val regions = visited.map { it.country.region }.toSet()
        val avg = if (visited.isNotEmpty()) {
            visited.sumOf { it.rating }.toFloat() / visited.size
        } else 0f
        return JournalStats(
            visitedCount = visited.size,
            regionsCount = regions.size,
            averageRating = avg,
            wishlistCount = wishlist.size,
            notesCount = notes.size
        )
    }

    fun onEvent(event: JournalEvent) {
        when (event) {
            is JournalEvent.RemoveVisit -> viewModelScope.launch {
                journalRepository.deleteVisit(event.countryCode)
            }
            is JournalEvent.RemoveWishlist -> viewModelScope.launch {
                journalRepository.deleteWishlist(event.countryCode)
            }
            is JournalEvent.RemoveNote -> viewModelScope.launch {
                journalRepository.deleteNote(event.countryCode)
            }
        }
    }
}
