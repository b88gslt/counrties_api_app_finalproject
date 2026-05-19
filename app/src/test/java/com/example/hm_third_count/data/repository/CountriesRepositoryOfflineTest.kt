package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.model.CountryFlags
import com.example.hm_third_count.data.model.CountryName
import com.example.hm_third_count.testutil.FakeCountriesApi
import com.example.hm_third_count.testutil.FakeCountryCacheDao
import com.example.hm_third_count.testutil.FakeFavoriteDao
import com.example.hm_third_count.testutil.MainDispatcherRule
import com.example.hm_third_count.testutil.TestClock
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test

/**
 * Integration: Repository + Fake API + Fake Room.
 * Закрывает требование ТЗ финального проекта «тесты на offline / sync».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CountriesRepositoryOfflineTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }
    private val profile = ProfileEntity(1L, "A", "#1", 0L)

    private val germany = country("DEU", "Germany", "Europe")
    private val usa = country("USA", "United States", "Americas")
    private val sample = listOf(germany, usa)

    private fun country(code: String, name: String, region: String) = Country(
        name = CountryName(common = name, official = name),
        code = code,
        capital = null,
        region = region,
        subregion = null,
        population = 1L,
        area = 1.0,
        flags = CountryFlags(png = "p", svg = "s", alt = null),
        languages = null,
        currencies = null,
        timezones = null,
        borders = null
    )

    private fun buildRepo(): RepoEnv {
        val api = FakeCountriesApi()
        val favoriteDao = FakeFavoriteDao()
        val cacheDao = FakeCountryCacheDao()
        val preferences = mockk<CountriesPreferences>(relaxed = true)
        every { preferences.sortCountriesAz } returns flowOf(false)
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(profile)
        val clock = TestClock()
        val repo = CountriesRepository(
            api = api,
            favoriteDao = favoriteDao,
            countryCacheDao = cacheDao,
            json = json,
            preferences = preferences,
            profileRepository = profileRepo,
            clock = clock
        )
        return RepoEnv(repo, api, cacheDao)
    }

    private data class RepoEnv(
        val repo: CountriesRepository,
        val api: FakeCountriesApi,
        val cacheDao: FakeCountryCacheDao
    )

    @Test
    fun `successful getAllCountries warms up cache for offline-first`() = runTest {
        val env = buildRepo()
        env.api.nextResponse = FakeCountriesApi.ApiResponse.Success(sample)

        val result = env.repo.getAllCountries()

        assertThat(result.isSuccess).isTrue()
        assertThat(env.cacheDao.snapshot().map { it.countryCode }).containsExactly("DEU", "USA")
    }

    @Test
    fun `getAllCountries falls back to cache on network error`() = runTest {
        val env = buildRepo()
        // Шаг 1: успешный запрос наполняет кэш.
        env.api.nextResponse = FakeCountriesApi.ApiResponse.Success(sample)
        env.repo.getAllCountries()

        // Шаг 2: сеть упала.
        env.api.nextResponse = FakeCountriesApi.ApiResponse.NetworkError
        val result = env.repo.getAllCountries()

        // Должен вернуть кэшированный список, а не Failure.
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.map { it.code }).containsExactly("DEU", "USA")
    }

    @Test
    fun `getAllCountries returns Failure when both network down AND cache empty`() = runTest {
        val env = buildRepo()
        env.api.nextResponse = FakeCountriesApi.ApiResponse.NetworkError

        val result = env.repo.getAllCountries()

        assertThat(result.isFailure).isTrue()
        assertThat(env.cacheDao.snapshot()).isEmpty()
    }

    @Test
    fun `searchCountries performs local search by name when network fails`() = runTest {
        val env = buildRepo()
        // Прогреваем кэш.
        env.api.nextResponse = FakeCountriesApi.ApiResponse.Success(sample)
        env.repo.getAllCountries()

        // Сеть упала.
        env.api.nextResponse = FakeCountriesApi.ApiResponse.NetworkError
        val result = env.repo.searchCountries("germ")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.map { it.code }).containsExactly("DEU")
    }

    @Test
    fun `getCountriesByRegion falls back to cache filtered by region`() = runTest {
        val env = buildRepo()
        env.api.nextResponse = FakeCountriesApi.ApiResponse.Success(sample)
        env.repo.getAllCountries()

        env.api.nextResponse = FakeCountriesApi.ApiResponse.NetworkError
        val result = env.repo.getCountriesByRegion("Europe")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.map { it.code }).containsExactly("DEU")
    }

    @Test
    fun `addToFavorites stores under active profile`() = runTest {
        val env = buildRepo()
        env.repo.addToFavorites(germany)

        assertThat(env.repo.isFavorite("DEU")).isTrue()
        assertThat(env.repo.isFavorite("USA")).isFalse()
    }
}
