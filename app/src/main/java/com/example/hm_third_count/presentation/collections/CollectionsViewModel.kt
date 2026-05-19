package com.example.hm_third_count.presentation.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.repository.CollectionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val collectionsRepository: CollectionsRepository
) : ViewModel() {

    /**
     * Реактивная сборка списка коллекций со счётчиком стран через combine двух Flow:
     *  - сами коллекции (collections)
     *  - все элементы (allItems — JOIN с фильтром по profileId)
     * Группируем allItems по collectionId и присоединяем к каждой коллекции.
     */
    val uiState: StateFlow<CollectionsUiState> = combine(
        collectionsRepository.collections,
        collectionsRepository.allItems
    ) { collections, items ->
        val countByCollection = items.groupingBy { it.collectionId }.eachCount()
        val summaries = collections.map { c ->
            CollectionSummary(
                id = c.id,
                name = c.name,
                colorHex = c.colorHex,
                countryCount = countByCollection[c.id] ?: 0
            )
        }
        CollectionsUiState(isLoading = false, collections = summaries)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CollectionsUiState(isLoading = true)
    )

    fun onEvent(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.Create -> viewModelScope.launch {
                if (event.name.isNotBlank()) {
                    collectionsRepository.create(event.name, event.colorHex)
                }
            }
            is CollectionsEvent.Rename -> viewModelScope.launch {
                collectionsRepository.rename(event.id, event.name)
            }
            is CollectionsEvent.Delete -> viewModelScope.launch {
                collectionsRepository.delete(event.id)
            }
        }
    }
}
