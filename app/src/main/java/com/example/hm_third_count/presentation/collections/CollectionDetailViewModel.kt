package com.example.hm_third_count.presentation.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.local.CollectionEntity
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.CountryCacheEntity
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.repository.CollectionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class CollectionDetailUiState(
    val isLoading: Boolean = true,
    val collection: CollectionEntity? = null,
    val countries: List<Country> = emptyList()
)

sealed class CollectionDetailEvent {
    data class RemoveCountry(val countryCode: String) : CollectionDetailEvent()
    data class Rename(val newName: String, val newColor: String) : CollectionDetailEvent()
    data object Delete : CollectionDetailEvent()
}

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val collectionsRepository: CollectionsRepository,
    private val countryCacheDao: CountryCacheDao,
    private val json: Json,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val collectionId: Long = checkNotNull(savedStateHandle["collectionId"])

    /**
     * combine 3 источников Room:
     *  - collection (метаданные: имя, цвет)
     *  - countryCodes в этой коллекции
     *  - countryCache (где данные стран)
     */
    val uiState: StateFlow<CollectionDetailUiState> = combine(
        collectionsRepository.observeCollection(collectionId),
        collectionsRepository.observeCountryCodesInCollection(collectionId),
        countryCacheDao.observeAll()
    ) { collection, codes, cache ->
        val cacheByCode = cache.associateBy { it.countryCode }
        val countries = codes.mapNotNull { code -> decode(cacheByCode[code]) }
        CollectionDetailUiState(
            isLoading = false,
            collection = collection,
            countries = countries
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CollectionDetailUiState(isLoading = true)
    )

    private fun decode(entity: CountryCacheEntity?): Country? {
        if (entity == null) return null
        return runCatching { json.decodeFromString<Country>(entity.json) }.getOrNull()
    }

    fun onEvent(event: CollectionDetailEvent) {
        when (event) {
            is CollectionDetailEvent.RemoveCountry -> viewModelScope.launch {
                collectionsRepository.removeCountry(collectionId, event.countryCode)
            }
            is CollectionDetailEvent.Rename -> viewModelScope.launch {
                collectionsRepository.rename(collectionId, event.newName)
                collectionsRepository.changeColor(collectionId, event.newColor)
            }
            CollectionDetailEvent.Delete -> viewModelScope.launch {
                collectionsRepository.delete(collectionId)
            }
        }
    }
}
