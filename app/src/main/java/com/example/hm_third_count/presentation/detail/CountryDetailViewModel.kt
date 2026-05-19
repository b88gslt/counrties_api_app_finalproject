package com.example.hm_third_count.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.cache.CachePolicy
import com.example.hm_third_count.data.cache.CacheStrategy
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.CountryNoteEntity
import com.example.hm_third_count.data.local.VisitEntity
import com.example.hm_third_count.data.local.WishlistEntity
import com.example.hm_third_count.data.repository.Clock
import com.example.hm_third_count.data.repository.CollectionsRepository
import com.example.hm_third_count.data.repository.CountriesRepository
import com.example.hm_third_count.data.repository.JournalRepository
import com.example.hm_third_count.data.repository.RecentViewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private sealed class FetchState {
    data object Idle : FetchState()
    data object Refreshing : FetchState()
    data class Error(val message: String) : FetchState()
}

private data class JournalSnapshot(
    val visit: VisitEntity?,
    val wishlist: WishlistEntity?,
    val note: CountryNoteEntity?
)

private data class CollectionsSnapshot(
    val allCollections: List<CollectionSummaryView>,
    val memberOf: Set<Long>
)

private data class NetworkFavoritesSnapshot(
    val cache: com.example.hm_third_count.data.local.CountryCacheEntity?,
    val fetch: FetchState,
    val favorites: Set<String>,
    val settings: com.example.hm_third_count.data.local.AppSettings
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CountryDetailViewModel @Inject constructor(
    private val repository: CountriesRepository,
    private val cachePolicy: CachePolicy,
    private val clock: Clock,
    private val preferences: CountriesPreferences,
    private val recentViewsRepository: RecentViewsRepository,
    private val journalRepository: JournalRepository,
    private val collectionsRepository: CollectionsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val countryCode: String = checkNotNull(savedStateHandle["countryCode"])

    /** Серия триггеров загрузки: первый старт + retry + manual refresh. true = forceRefresh. */
    private val fetchTriggers = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Текущее состояние сетевой загрузки поверх кэша. */
    private val fetchState = MutableStateFlow<FetchState>(FetchState.Idle)

    /** Кэш из Room — основной источник данных. */
    private val cachedCountry = repository.observeCachedCountry(countryCode)

    /** Журнальный слой для этой страны: visit + wishlist + note. */
    private val journalSnapshot = combine(
        journalRepository.observeVisit(countryCode),
        journalRepository.observeWishlist(countryCode),
        journalRepository.observeNote(countryCode)
    ) { visit, wishlist, note ->
        JournalSnapshot(visit, wishlist, note)
    }

    /** Коллекции активного профиля + членство этой страны в них. */
    private val collectionsSnapshot = combine(
        collectionsRepository.collections,
        collectionsRepository.observeCollectionsForCountry(countryCode)
    ) { allCollections, memberOfIds ->
        CollectionsSnapshot(
            allCollections = allCollections.map {
                CollectionSummaryView(it.id, it.name, it.colorHex)
            },
            memberOf = memberOfIds.toSet()
        )
    }

    private val networkAndFavorites = combine(
        cachedCountry,
        fetchState,
        repository.favorites,
        preferences.settings
    ) { cache, fetch, favorites, settings ->
        NetworkFavoritesSnapshot(cache, fetch, favorites, settings)
    }

    val uiState: StateFlow<CountryDetailUiState> = combine(
        networkAndFavorites,
        journalSnapshot,
        collectionsSnapshot
    ) { netPart, journal, collections ->
        val cache = netPart.cache
        val fetch = netPart.fetch
        val favorites = netPart.favorites
        val settings = netPart.settings

        val country = cache?.let(repository::decodeCachedCountry)
        val fetchedAt = cache?.fetchedAt
        val isStale = fetchedAt?.let {
            cachePolicy.isStale(it, clock.nowMillis(), settings.cacheTtlHours)
        } ?: false
        val isFavorite = countryCode in favorites

        val visitView = journal.visit?.let {
            VisitView(visitedAt = it.visitedAt, rating = it.rating, note = it.note)
        }
        val wishlistView = journal.wishlist?.let {
            WishlistView(priority = it.priority, plannedDate = it.plannedDate)
        }
        val noteText = journal.note?.text

        when {
            country != null -> CountryDetailUiState(
                isLoading = false,
                country = country,
                error = null,
                isFavorite = isFavorite,
                fetchedAt = fetchedAt,
                isStale = isStale,
                isRefreshing = fetch is FetchState.Refreshing,
                visit = visitView,
                wishlist = wishlistView,
                note = noteText,
                allCollections = collections.allCollections,
                memberOfCollections = collections.memberOf,
                areaUnit = settings.areaUnit
            )
            fetch is FetchState.Refreshing -> CountryDetailUiState(
                isLoading = true,
                isFavorite = isFavorite,
                visit = visitView,
                wishlist = wishlistView,
                note = noteText,
                allCollections = collections.allCollections,
                memberOfCollections = collections.memberOf,
                areaUnit = settings.areaUnit
            )
            fetch is FetchState.Error -> CountryDetailUiState(
                isLoading = false,
                error = fetch.message,
                isFavorite = isFavorite,
                visit = visitView,
                wishlist = wishlistView,
                note = noteText,
                allCollections = collections.allCollections,
                memberOfCollections = collections.memberOf,
                areaUnit = settings.areaUnit
            )
            else -> CountryDetailUiState(
                isLoading = true,
                isFavorite = isFavorite,
                visit = visitView,
                wishlist = wishlistView,
                note = noteText,
                allCollections = collections.allCollections,
                memberOfCollections = collections.memberOf,
                areaUnit = settings.areaUnit
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CountryDetailUiState(isLoading = true)
    )

    init {
        fetchTriggers
            .onStart { emit(false) }
            .scan(0 to false) { acc, force -> (acc.first + 1) to force }
            .onEach { (attempt, force) -> if (attempt > 0) runFetchIfNeeded(force) }
            .launchIn(viewModelScope)

        viewModelScope.launch { recentViewsRepository.recordView(countryCode) }
    }

    private suspend fun runFetchIfNeeded(forceRefresh: Boolean) {
        val ttlHours = preferences.settings.first().cacheTtlHours
        val cached = repository.observeCachedCountry(countryCode).first()
        val strategy = cachePolicy.strategy(
            cachedAt = cached?.fetchedAt,
            now = clock.nowMillis(),
            ttlHours = ttlHours,
            forceRefresh = forceRefresh
        )
        when (strategy) {
            CacheStrategy.UseCacheOnly -> {
                fetchState.value = FetchState.Idle
            }
            CacheStrategy.FetchOnly,
            CacheStrategy.UseCacheAndRevalidate -> {
                fetchState.value = FetchState.Refreshing
                repository.getCountryByCode(countryCode)
                    .onSuccess { country ->
                        fetchState.value = if (country == null && cached == null) {
                            FetchState.Error("Country not found")
                        } else {
                            FetchState.Idle
                        }
                    }
                    .onFailure { e ->
                        fetchState.value = if (cached == null) {
                            FetchState.Error(e.message ?: "Failed to load country details")
                        } else {
                            FetchState.Idle
                        }
                    }
            }
        }
    }

    fun onEvent(event: CountryDetailEvent) {
        when (event) {
            CountryDetailEvent.Retry -> {
                viewModelScope.launch { fetchTriggers.emit(false) }
            }
            CountryDetailEvent.ManualRefresh -> {
                viewModelScope.launch { fetchTriggers.emit(true) }
            }
            CountryDetailEvent.ToggleFavorite -> toggleFavorite()
            is CountryDetailEvent.SaveVisit -> viewModelScope.launch {
                journalRepository.upsertVisit(countryCode, event.visitedAt, event.rating, event.note)
            }
            CountryDetailEvent.DeleteVisit -> viewModelScope.launch {
                journalRepository.deleteVisit(countryCode)
            }
            is CountryDetailEvent.SaveWishlist -> viewModelScope.launch {
                journalRepository.upsertWishlist(countryCode, event.priority, event.plannedDate)
            }
            CountryDetailEvent.DeleteWishlist -> viewModelScope.launch {
                journalRepository.deleteWishlist(countryCode)
            }
            is CountryDetailEvent.SaveNote -> viewModelScope.launch {
                journalRepository.upsertNote(countryCode, event.text)
            }
            CountryDetailEvent.DeleteNote -> viewModelScope.launch {
                journalRepository.deleteNote(countryCode)
            }
            is CountryDetailEvent.SetCollections -> viewModelScope.launch {
                collectionsRepository.setCountryMembership(countryCode, event.selectedIds)
            }
            is CountryDetailEvent.CreateAndAddCollection -> viewModelScope.launch {
                val newId = collectionsRepository.create(event.name, event.colorHex)
                if (newId > 0) collectionsRepository.addCountry(newId, countryCode)
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val country = uiState.value.country ?: return@launch
            if (repository.isFavorite(countryCode)) repository.removeFromFavorites(countryCode)
            else repository.addToFavorites(country)
        }
    }
}
