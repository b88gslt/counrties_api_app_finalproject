package com.example.hm_third_count.presentation.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.repository.RecentViewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val recentViewsRepository: RecentViewsRepository,
    private val countryCacheDao: CountryCacheDao,
    private val json: Json
) : ViewModel() {

    /**
     * Реактивная сборка из двух источников Room:
     *  - recentViewsRepository.recentForActiveProfile (история, реагирует на смену профиля)
     *  - countryCacheDao.observeAll() (кэшированные страны)
     * Join на стороне kotlin: по countryCode, в порядке убывания viewedAt.
     */
    val uiState: StateFlow<RecentUiState> = combine(
        recentViewsRepository.recentForActiveProfile,
        countryCacheDao.observeAll()
    ) { recents, cached ->
        val cacheByCode = cached.associateBy { it.countryCode }
        val items = recents.mapNotNull { recent ->
            val entity = cacheByCode[recent.countryCode] ?: return@mapNotNull null
            val country = runCatching { json.decodeFromString<Country>(entity.json) }.getOrNull()
                ?: return@mapNotNull null
            RecentItem(country = country, viewedAt = recent.viewedAt)
        }
        RecentUiState(isLoading = false, items = items)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RecentUiState(isLoading = true)
    )

    fun onEvent(event: RecentEvent) {
        when (event) {
            RecentEvent.ClearAll -> viewModelScope.launch { recentViewsRepository.clearAll() }
            is RecentEvent.Remove -> viewModelScope.launch {
                recentViewsRepository.delete(event.countryCode)
            }
        }
    }
}
