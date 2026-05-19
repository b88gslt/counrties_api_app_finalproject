package com.example.hm_third_count.presentation.countries

import com.example.hm_third_count.data.model.Country

data class SavedFilterView(
    val id: Long,
    val name: String,
    val region: String,
    val showFavoritesOnly: Boolean
)

data class CountriesUiState(
    val isLoading: Boolean = false,
    val countries: List<Country> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val selectedRegion: String = "",
    val favorites: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    /** Настройка из DataStore: сортировка по имени A–Z. */
    val sortByNameAz: Boolean = false,
    /** Коды посещённых стран активного профиля — для visited-бейджа на карточках. */
    val visitedCodes: Set<String> = emptySet(),
    /** Настройка показа visited-бейджа на карточке (из DataStore). */
    val showVisitedBadge: Boolean = true,
    /** Пресеты фильтра активного профиля. */
    val savedFilters: List<SavedFilterView> = emptyList(),
    /** Идёт ли сейчас фоновое обновление (WorkManager). */
    val isBackgroundSyncing: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && countries.isEmpty() && error == null && !showFavoritesOnly

    /** Текущий фильтр — есть что сохранять, если выбран регион или фавориты. */
    val canSaveCurrentAsPreset: Boolean
        get() = selectedRegion.isNotBlank() || showFavoritesOnly
}

sealed class CountriesEvent {
    data class SearchQueryChanged(val query: String) : CountriesEvent()
    data class RegionSelected(val region: String) : CountriesEvent()
    data class ToggleFavorite(val country: Country) : CountriesEvent()
    object ShowFavorites : CountriesEvent()
    object Retry : CountriesEvent()
    object ToggleSortAz : CountriesEvent()
    data class SaveCurrentAsPreset(val name: String) : CountriesEvent()
    data class ApplyPreset(val id: Long) : CountriesEvent()
    data class DeletePreset(val id: Long) : CountriesEvent()
}
