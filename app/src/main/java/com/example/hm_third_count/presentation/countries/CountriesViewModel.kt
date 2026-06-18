package com.example.hm_third_count.presentation.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.repository.CountriesRepository
import com.example.hm_third_count.data.repository.JournalRepository
import com.example.hm_third_count.data.repository.SavedFiltersRepository
import com.example.hm_third_count.sync.SyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Экран списка стран: один [CountriesUiState] собирается из независимых источников.
 *
 * **Инварианты UI:**
 * - при непустом поиске показываем только результат поиска (без региональных фильтров);
 * - при пустом поиске — полный список + фильтр региона / избранного;
 * - избранное, visited-бейджи и пресеты всегда от активного профиля;
 * - ошибка сети не перекрывает успешный offline-кэш, пустой результат поиска — не ошибка.
 *
 * Реактивная сборка: merge(refresh) + flatMapLatest для загрузки, debounce для поиска,
 * combine для фильтров/настроек/journal/sync, StateFlow — итоговое состояние экрана.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val repository: CountriesRepository,
    private val journalRepository: JournalRepository,
    private val savedFiltersRepository: SavedFiltersRepository,
    private val preferences: CountriesPreferences,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searchRetry = MutableStateFlow(0)
    private val _filter = MutableStateFlow(ListFilter())

    /** Поток перезагрузок полного списка (Retry и т.д.): осмысленная серия триггеров. */
    private val refreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private sealed class AllCountriesState {
        data object Loading : AllCountriesState()
        data class Success(val countries: List<Country>) : AllCountriesState()
        data class Error(val message: String) : AllCountriesState()
    }

    private sealed class SearchSlice {
        data object Inactive : SearchSlice()
        data object Loading : SearchSlice()
        data class Success(val countries: List<Country>) : SearchSlice()
        data class Error(val message: String) : SearchSlice()
    }

    private data class ListFilter(
        val region: String = "",
        val showFavoritesOnly: Boolean = false
    )

    /**
     * merge(старт, refresh) → **scan** (номер попытки) → **filter** (>0, чтобы убрать начальный 0 до первого события)
     * → **flatMapLatest** к сети.
     */
    private val loadAllTriggers = merge(flowOf(Unit), refreshSignal)
        .scan(0) { acc, _ -> acc + 1 }
        .filter { it > 0 }

    private val allCountriesState: StateFlow<AllCountriesState> = loadAllTriggers
        .flatMapLatest {
            flow<AllCountriesState> {
                emit(AllCountriesState.Loading)
                repository.getAllCountries()
                    .onSuccess { emit(AllCountriesState.Success(it)) }
                    .onFailure { emit(AllCountriesState.Error(it.message ?: "Unknown error")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AllCountriesState.Loading)

    private val debouncedQuery = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()

    /** query → debounce → distinctUntilChanged; combine со счётчиком retry; flatMapLatest → поиск. */
    private val searchPipeline: StateFlow<SearchSlice> = combine(debouncedQuery, _searchRetry) { q, r -> q to r }
        .flatMapLatest { (q, _) ->
            if (q.isBlank()) {
                flowOf<SearchSlice>(SearchSlice.Inactive)
            } else {
                flow<SearchSlice> {
                    emit(SearchSlice.Loading)
                    repository.searchCountries(q)
                        .onSuccess { emit(SearchSlice.Success(it)) }
                        .onFailure { emit(SearchSlice.Error(it.message ?: "Search failed")) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SearchSlice.Inactive)

    /** Избранное активного профиля — коды и offline-снимки из одной подписки Room. */
    private val favoritesBundle = repository.favoritesBundle

    /**
     * Journal + presets + sync источник: visited-коды активного профиля, флаг показа бейджа,
     * список пресетов, индикатор фоновой синхронизации.
     */
    private val journalAndPresetsPart = combine(
        journalRepository.visits,
        preferences.settings,
        savedFiltersRepository.savedFilters,
        syncCoordinator.syncRunning
    ) { visits, settings, presets, syncRunning ->
        JournalAndPresets(
            visitedCodes = visits.map { it.countryCode }.toSet(),
            showVisitedBadge = settings.showVisitedBadge,
            presets = presets.map { SavedFilterView(it.id, it.name, it.region, it.showFavoritesOnly) },
            isSyncing = syncRunning
        )
    }

    val uiState: StateFlow<CountriesUiState> = combine(
        combine(allCountriesState, searchPipeline, _filter) { all, search, filter ->
            Triple(all, search, filter)
        },
        combine(favoritesBundle, _searchQuery) { bundle, raw ->
            Triple(bundle.codes, bundle.snapshots, raw)
        },
        repository.sortCountriesAz,
        journalAndPresetsPart
    ) { listPart, favPart, sortAz, journalPart ->
        buildUiState(
            listPart.first,
            listPart.second,
            listPart.third,
            favPart.first,
            favPart.second,
            favPart.third,
            sortAz,
            journalPart.visitedCodes,
            journalPart.showVisitedBadge,
            journalPart.presets,
            journalPart.isSyncing
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CountriesUiState(isLoading = true)
    )

    private data class JournalAndPresets(
        val visitedCodes: Set<String>,
        val showVisitedBadge: Boolean,
        val presets: List<SavedFilterView>,
        val isSyncing: Boolean
    )

    fun onEvent(event: CountriesEvent) {
        when (event) {
            is CountriesEvent.SearchQueryChanged -> {
                _searchQuery.value = event.query
                _filter.value = ListFilter()
            }
            is CountriesEvent.RegionSelected -> {
                val current = _filter.value
                if (current.region == event.region && !current.showFavoritesOnly) {
                    _filter.value = ListFilter()
                } else {
                    _filter.value = ListFilter(region = event.region, showFavoritesOnly = false)
                }
                _searchQuery.value = ""
            }
            CountriesEvent.ShowFavorites -> {
                _filter.value = ListFilter(showFavoritesOnly = true)
                _searchQuery.value = ""
            }
            is CountriesEvent.ToggleFavorite -> toggleFavorite(event.country)
            CountriesEvent.Retry -> {
                viewModelScope.launch {
                    refreshSignal.emit(Unit)
                    _searchRetry.value = _searchRetry.value + 1
                }
            }
            CountriesEvent.ToggleSortAz -> {
                viewModelScope.launch {
                    val cur = repository.sortCountriesAz.first()
                    repository.setSortCountriesAz(!cur)
                }
            }
            is CountriesEvent.SaveCurrentAsPreset -> {
                val current = _filter.value
                if (event.name.isNotBlank() && (current.region.isNotBlank() || current.showFavoritesOnly)) {
                    viewModelScope.launch {
                        savedFiltersRepository.save(
                            name = event.name,
                            region = current.region,
                            showFavoritesOnly = current.showFavoritesOnly
                        )
                    }
                }
            }
            is CountriesEvent.ApplyPreset -> {
                val preset = uiState.value.savedFilters.firstOrNull { it.id == event.id } ?: return
                _filter.value = ListFilter(
                    region = preset.region,
                    showFavoritesOnly = preset.showFavoritesOnly
                )
                _searchQuery.value = ""
            }
            is CountriesEvent.DeletePreset -> {
                viewModelScope.launch { savedFiltersRepository.delete(event.id) }
            }
        }
    }

    private fun applySortAz(list: List<Country>, sortAz: Boolean): List<Country> =
        if (sortAz) list.sortedBy { it.name.common.lowercase() } else list

    private fun favoriteDisplayList(
        favCodes: Set<String>,
        all: List<Country>,
        snapshots: List<Country>
    ): List<Country> =
        if (all.isNotEmpty()) {
            all.filter { it.code in favCodes }
        } else {
            snapshots.filter { it.code in favCodes }
        }

    private fun buildUiState(
        all: AllCountriesState,
        search: SearchSlice,
        filter: ListFilter,
        favCodes: Set<String>,
        snapshots: List<Country>,
        rawQuery: String,
        sortAz: Boolean,
        visitedCodes: Set<String>,
        showVisitedBadge: Boolean,
        presets: List<SavedFilterView>,
        isBackgroundSyncing: Boolean
    ): CountriesUiState {
        fun base(
            countries: List<Country>,
            loading: Boolean,
            err: String?,
            region: String = filter.region,
            favOnly: Boolean = filter.showFavoritesOnly
        ) = CountriesUiState(
            isLoading = loading,
            countries = applySortAz(countries, sortAz),
            error = err,
            searchQuery = rawQuery,
            selectedRegion = region,
            showFavoritesOnly = favOnly,
            favorites = favCodes,
            sortByNameAz = sortAz,
            visitedCodes = visitedCodes,
            showVisitedBadge = showVisitedBadge,
            savedFilters = presets,
            isBackgroundSyncing = isBackgroundSyncing
        )

        when (search) {
            SearchSlice.Loading ->
                return base(emptyList(), true, null)

            is SearchSlice.Error ->
                return base(emptyList(), false, search.message)

            is SearchSlice.Success ->
                return base(
                    countries = search.countries,
                    loading = false,
                    err = null,
                    region = "",
                    favOnly = false
                )

            SearchSlice.Inactive -> when (all) {
                AllCountriesState.Loading ->
                    return base(emptyList(), true, null)

                is AllCountriesState.Error ->
                    return base(emptyList(), false, all.message)

                is AllCountriesState.Success -> {
                    val list = when {
                        filter.showFavoritesOnly ->
                            favoriteDisplayList(favCodes, all.countries, snapshots)
                        filter.region.isNotEmpty() ->
                            all.countries.filter { it.region.equals(filter.region, ignoreCase = true) }
                        else -> all.countries
                    }
                    return base(list, false, null)
                }
            }
        }
    }

    private fun toggleFavorite(country: Country) {
        viewModelScope.launch {
            if (repository.isFavorite(country.code)) repository.removeFromFavorites(country.code)
            else repository.addToFavorites(country)
        }
    }
}
