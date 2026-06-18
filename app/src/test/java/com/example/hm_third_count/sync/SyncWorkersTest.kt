package com.example.hm_third_count.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.hm_third_count.data.local.AppSettings
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.CountryCacheEntity
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.data.local.VisitEntity
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.data.model.CountryFlags
import com.example.hm_third_count.data.model.CountryName
import com.example.hm_third_count.data.repository.CountriesRepository
import com.example.hm_third_count.data.repository.ProfileRepository
import com.example.hm_third_count.testutil.FakeCollectionItemDao
import com.example.hm_third_count.testutil.FakeCountriesApi
import com.example.hm_third_count.testutil.FakeCountryCacheDao
import com.example.hm_third_count.testutil.FakeFavoriteDao
import com.example.hm_third_count.testutil.FakeVisitDao
import com.example.hm_third_count.testutil.MainDispatcherRule
import com.example.hm_third_count.testutil.TestClock
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncWorkersTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val appContext = mockk<Context>(relaxed = true)
    private val workerParams = mockk<WorkerParameters>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = TestClock()

    private val germany = Country(
        name = CountryName("Germany", "Germany"),
        code = "DEU",
        capital = null,
        region = "Europe",
        subregion = null,
        population = 1L,
        area = 1.0,
        flags = CountryFlags("p", "s", null),
        languages = null,
        currencies = null,
        timezones = null,
        borders = null
    )

    @Test
    fun `WarmupAllCountriesWorker succeeds and updates last sync timestamp`() = runTest {
        val api = FakeCountriesApi()
        api.nextResponse = FakeCountriesApi.ApiResponse.Success(listOf(germany))
        val cacheDao = FakeCountryCacheDao()
        val preferences = mockk<CountriesPreferences>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(ProfileEntity(1L, "A", "#1", 0L))
        val repository = CountriesRepository(
            api = api,
            favoriteDao = FakeFavoriteDao(),
            countryCacheDao = cacheDao,
            json = json,
            preferences = preferences,
            profileRepository = profileRepo,
            clock = clock
        )

        val worker = WarmupAllCountriesWorker(appContext, workerParams, repository, preferences, clock)

        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.success())
        coVerify { preferences.setLastFullSyncAt(clock.nowMillis()) }
        assertThat(cacheDao.snapshot()).isNotEmpty()
    }

    @Test
    fun `WarmupAllCountriesWorker retries when network fails`() = runTest {
        val api = FakeCountriesApi()
        api.nextResponse = FakeCountriesApi.ApiResponse.NetworkError
        val preferences = mockk<CountriesPreferences>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(ProfileEntity(1L, "A", "#1", 0L))
        val repository = CountriesRepository(
            api = api,
            favoriteDao = FakeFavoriteDao(),
            countryCacheDao = FakeCountryCacheDao(),
            json = json,
            preferences = preferences,
            profileRepository = profileRepo,
            clock = clock
        )

        val worker = WarmupAllCountriesWorker(appContext, workerParams, repository, preferences, clock)

        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `RefreshCacheWorker refreshes pinned countries for active profile`() = runTest {
        val visitDao = FakeVisitDao()
        visitDao.upsert(
            VisitEntity(
                countryCode = "DEU",
                profileId = 1L,
                visitedAt = 1L,
                rating = 5,
                note = null,
                createdAt = 1L
            )
        )
        val api = FakeCountriesApi()
        api.nextResponse = FakeCountriesApi.ApiResponse.Success(listOf(germany))
        val preferences = mockk<CountriesPreferences>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(ProfileEntity(1L, "A", "#1", 0L))
        val repository = CountriesRepository(
            api = api,
            favoriteDao = FakeFavoriteDao(),
            countryCacheDao = FakeCountryCacheDao(),
            json = json,
            preferences = preferences,
            profileRepository = profileRepo,
            clock = clock
        )

        val worker = RefreshCacheWorker(
            appContext,
            workerParams,
            profileRepo,
            visitDao,
            mockk(relaxed = true),
            FakeCollectionItemDao(),
            repository,
            preferences,
            clock
        )

        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.success())
        assertThat(api.callCount).isEqualTo(1)
        coVerify { preferences.setLastFullSyncAt(clock.nowMillis()) }
    }

    @Test
    fun `CleanupStaleCacheWorker keeps cache pinned by inactive profile data`() = runTest {
        val visitDao = FakeVisitDao()
        visitDao.upsert(
            VisitEntity(
                countryCode = "DEU",
                profileId = 2L,
                visitedAt = 1L,
                rating = 4,
                note = null,
                createdAt = 1L
            )
        )
        val cacheDao = FakeCountryCacheDao()
        val staleAt = clock.nowMillis() - 30L * 24L * 60L * 60L * 1000L
        cacheDao.upsert(
            CountryCacheEntity(
                countryCode = "DEU",
                json = """{"name":{"common":"Germany","official":"Germany"},"cca3":"DEU","region":"Europe","population":1,"area":1.0,"flags":{"png":"p","svg":"s"}}""",
                fetchedAt = staleAt
            )
        )
        cacheDao.upsert(
            CountryCacheEntity(
                countryCode = "USA",
                json = """{"name":{"common":"Germany","official":"Germany"},"cca3":"USA","region":"Europe","population":1,"area":1.0,"flags":{"png":"p","svg":"s"}}""",
                fetchedAt = staleAt
            )
        )
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.profiles } returns flowOf(
            listOf(
                ProfileEntity(1L, "Active", "#1", 0L),
                ProfileEntity(2L, "Other", "#2", 1L)
            )
        )

        val worker = CleanupStaleCacheWorker(
            appContext,
            workerParams,
            cacheDao,
            profileRepo,
            visitDao,
            mockk(relaxed = true),
            FakeCollectionItemDao(),
            mockk(relaxed = true),
            clock
        )

        worker.doWork()

        assertThat(cacheDao.snapshot().map { it.countryCode }).containsExactly("DEU")
    }

    @Test
    fun `SyncCoordinator bootstrap enqueues warmup when cache is empty and schedules refresh`() = runTest {
        mockkStatic(WorkManager::class)
        try {
            val workManager = mockk<WorkManager>(relaxed = true)
            every { WorkManager.getInstance(any()) } returns workManager
            val preferences = mockk<CountriesPreferences>()
            coEvery { preferences.settings } returns flowOf(AppSettings(backgroundRefreshHours = 12))
            val cacheDao = mockk<CountryCacheDao>()
            every { cacheDao.observeAll() } returns flowOf(emptyList())

            SyncCoordinator(appContext, preferences, cacheDao).bootstrap()

            verify {
                workManager.enqueueUniqueWork(
                    "warmup_all_countries",
                    ExistingWorkPolicy.KEEP,
                    any<OneTimeWorkRequest>()
                )
            }
            verify {
                workManager.enqueueUniquePeriodicWork(
                    "refresh_cache_periodic",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    any<PeriodicWorkRequest>()
                )
            }
            verify {
                workManager.enqueueUniquePeriodicWork(
                    "cleanup_stale_cache",
                    ExistingPeriodicWorkPolicy.KEEP,
                    any<PeriodicWorkRequest>()
                )
            }
        } finally {
            unmockkStatic(WorkManager::class)
        }
    }

    @Test
    fun `SyncCoordinator rescheduleByPreference cancels refresh when disabled`() = runTest {
        mockkStatic(WorkManager::class)
        try {
            val workManager = mockk<WorkManager>(relaxed = true)
            every { WorkManager.getInstance(any()) } returns workManager
            val preferences = mockk<CountriesPreferences>()
            coEvery { preferences.settings } returns flowOf(AppSettings(backgroundRefreshHours = 0))
            val cacheDao = mockk<CountryCacheDao>()

            SyncCoordinator(appContext, preferences, cacheDao).rescheduleByPreference()

            verify { workManager.cancelUniqueWork("refresh_cache_periodic") }
        } finally {
            unmockkStatic(WorkManager::class)
        }
    }
}
