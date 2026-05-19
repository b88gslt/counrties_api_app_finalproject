package com.example.hm_third_count.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.example.hm_third_count.data.cache.CachePolicy
import com.example.hm_third_count.data.local.AppSettings
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.CountryCacheEntity
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.model.CountryFlags
import com.example.hm_third_count.data.model.CountryName
import com.example.hm_third_count.data.repository.CollectionsRepository
import com.example.hm_third_count.data.repository.CountriesRepository
import com.example.hm_third_count.data.repository.JournalRepository
import com.example.hm_third_count.data.repository.RecentViewsRepository
import com.example.hm_third_count.testutil.MainDispatcherRule
import com.example.hm_third_count.testutil.TestClock
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }
    private val germany = Country(
        name = CountryName(common = "Germany", official = "Federal Republic of Germany"),
        code = "DEU",
        capital = listOf("Berlin"),
        region = "Europe",
        subregion = null,
        population = 83_000_000L,
        area = 357_022.0,
        flags = CountryFlags(png = "p", svg = "s", alt = null),
        languages = null,
        currencies = null,
        timezones = null,
        borders = null
    )

    private fun freshCacheEntity(now: Long): CountryCacheEntity =
        CountryCacheEntity("DEU", json.encodeToString(Country.serializer(), germany), now)

    private fun staleCacheEntity(now: Long): CountryCacheEntity =
        CountryCacheEntity(
            "DEU",
            json.encodeToString(Country.serializer(), germany),
            now - 48L * 3600L * 1000L
        )

    private fun build(
        cache: CountryCacheEntity?,
        clock: TestClock = TestClock(initial = 1_000_000_000L),
        repositoryGetByCodeAnswer: Result<Country?> = Result.success(germany)
    ): TestEnv {
        val cacheFlow = MutableStateFlow(cache)
        val repository = mockk<CountriesRepository>()
        every { repository.observeCachedCountry("DEU") } returns cacheFlow
        every { repository.favorites } returns flowOf(emptySet())
        every { repository.decodeCachedCountry(any()) } answers {
            val arg = firstArg<CountryCacheEntity>()
            runCatching { json.decodeFromString<Country>(arg.json) }.getOrNull()
        }
        coEvery { repository.getCountryByCode("DEU") } coAnswers {
            val r = repositoryGetByCodeAnswer
            r.onSuccess { c ->
                if (c != null) cacheFlow.value = CountryCacheEntity(
                    "DEU",
                    json.encodeToString(Country.serializer(), c),
                    clock.nowMillis()
                )
            }
            r
        }
        coEvery { repository.isFavorite(any()) } returns false
        coEvery { repository.addToFavorites(any()) } just Runs
        coEvery { repository.removeFromFavorites(any()) } just Runs

        val preferences = mockk<CountriesPreferences>()
        every { preferences.settings } returns flowOf(AppSettings(cacheTtlHours = 24))

        val recentRepo = mockk<RecentViewsRepository>(relaxed = true)
        coEvery { recentRepo.recordView(any()) } just Runs

        val journalRepo = mockk<JournalRepository>()
        every { journalRepo.observeVisit("DEU") } returns flowOf(null)
        every { journalRepo.observeWishlist("DEU") } returns flowOf(null)
        every { journalRepo.observeNote("DEU") } returns flowOf(null)

        val collectionsRepo = mockk<CollectionsRepository>()
        every { collectionsRepo.collections } returns flowOf(emptyList())
        every { collectionsRepo.observeCollectionsForCountry("DEU") } returns flowOf(emptyList())

        val vm = CountryDetailViewModel(
            repository = repository,
            cachePolicy = CachePolicy(),
            clock = clock,
            preferences = preferences,
            recentViewsRepository = recentRepo,
            journalRepository = journalRepo,
            collectionsRepository = collectionsRepo,
            savedStateHandle = SavedStateHandle(mapOf("countryCode" to "DEU"))
        )
        return TestEnv(vm, repository, cacheFlow)
    }

    private data class TestEnv(
        val vm: CountryDetailViewModel,
        val repository: CountriesRepository,
        val cacheFlow: MutableStateFlow<CountryCacheEntity?>
    )

    @Test
    fun `with fresh cache shows country and does NOT hit network`() = runTest {
        val clock = TestClock(initial = 1_000_000_000L)
        val env = build(cache = freshCacheEntity(clock.now), clock = clock)

        // UnconfinedTestDispatcher + SharingStarted.Eagerly ⇒ значение сразу актуально.
        val state = env.vm.uiState.first { it.country != null }
        assertThat(state.country?.code).isEqualTo("DEU")
        assertThat(state.isLoading).isFalse()
        assertThat(state.isStale).isFalse()
        assertThat(state.isRefreshing).isFalse()

        // Свежий кэш → сеть не дёргаем.
        coVerify(exactly = 0) { env.repository.getCountryByCode("DEU") }
    }

    @Test
    fun `with stale cache shows country immediately and revalidates in background`() = runTest {
        val clock = TestClock(initial = 1_000_000_000L)
        val env = build(cache = staleCacheEntity(clock.now), clock = clock)

        // Сначала видим страну (хоть и устаревшую).
        val withCountry = env.vm.uiState.first { it.country != null }
        assertThat(withCountry.country?.code).isEqualTo("DEU")

        // Сеть должна быть вызвана для revalidate.
        coVerify(atLeast = 1) { env.repository.getCountryByCode("DEU") }
    }

    @Test
    fun `with no cache and network error shows Error state with retry`() = runTest {
        val clock = TestClock(initial = 1_000_000_000L)
        val env = build(
            cache = null,
            clock = clock,
            repositoryGetByCodeAnswer = Result.failure(RuntimeException("No internet"))
        )

        val terminal = env.vm.uiState.first { it.error != null }
        assertThat(terminal.error).isEqualTo("No internet")
        assertThat(terminal.country).isNull()
        assertThat(terminal.isLoading).isFalse()
    }

    @Test
    fun `retry after error triggers a new fetch`() = runTest {
        val clock = TestClock(initial = 1_000_000_000L)
        val env = build(
            cache = null,
            clock = clock,
            repositoryGetByCodeAnswer = Result.failure(RuntimeException("No internet"))
        )

        // Ждём первую ошибку.
        env.vm.uiState.first { it.error != null }

        env.vm.onEvent(CountryDetailEvent.Retry)
        // Ждём чтобы retry успел оформиться (snapshot должен снова перейти в ошибку).
        env.vm.uiState.first { it.error != null }

        // Минимум 2 вызова: первоначальный и retry.
        coVerify(atLeast = 2) { env.repository.getCountryByCode("DEU") }
    }
}
