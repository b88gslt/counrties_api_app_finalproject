package com.example.hm_third_count.data.repository

import app.cash.turbine.test
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.testutil.FakeRecentViewDao
import com.example.hm_third_count.testutil.MainDispatcherRule
import com.example.hm_third_count.testutil.TestClock
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecentViewsRepositoryTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val profileA = ProfileEntity(id = 1L, name = "A", colorHex = "#1", createdAt = 0L)
    private val profileB = ProfileEntity(id = 2L, name = "B", colorHex = "#2", createdAt = 0L)

    private fun buildRepo(
        activeProfile: ProfileEntity? = profileA,
        clock: TestClock = TestClock()
    ): Pair<RecentViewsRepository, FakeRecentViewDao> {
        val dao = FakeRecentViewDao()
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(activeProfile)
        return RecentViewsRepository(
            recentViewDao = dao,
            profileRepository = profileRepo,
            clock = clock
        ) to dao
    }

    @Test
    fun `recordView creates a single row for first view`() = runTest {
        val (repo, dao) = buildRepo()
        repo.recordView("DEU")
        assertThat(dao.snapshot()).hasSize(1)
        assertThat(dao.snapshot().first().countryCode).isEqualTo("DEU")
    }

    @Test
    fun `recordView is deduplicated by composite PK (code+profile)`() = runTest {
        val (repo, dao) = buildRepo()
        // Открываем одну и ту же страну 5 раз подряд.
        repeat(5) { repo.recordView("DEU") }
        // PRIMARY KEY (countryCode, profileId) + REPLACE — должна быть ровно одна запись.
        assertThat(dao.snapshot()).hasSize(1)
    }

    @Test
    fun `recordView updates viewedAt on subsequent views`() = runTest {
        val clock = TestClock(initial = 1_000_000L)
        val (repo, dao) = buildRepo(clock = clock)
        repo.recordView("DEU")
        val firstAt = dao.snapshot().first().viewedAt

        clock.advance(60_000L) // прошла минута
        repo.recordView("DEU")
        val secondAt = dao.snapshot().first().viewedAt

        assertThat(secondAt).isGreaterThan(firstAt)
        assertThat(secondAt - firstAt).isEqualTo(60_000L)
    }

    @Test
    fun `recentForActiveProfile reflects only active profile data`() = runTest {
        // У repo один профиль активный, но в DAO есть просмотры от обоих.
        val dao = FakeRecentViewDao()
        // Записываем напрямую в DAO для обоих профилей.
        dao.upsert(
            com.example.hm_third_count.data.local.RecentViewEntity(
                "DEU", profileA.id, viewedAt = 100L
            )
        )
        dao.upsert(
            com.example.hm_third_count.data.local.RecentViewEntity(
                "USA", profileB.id, viewedAt = 200L
            )
        )

        val activeFlow = MutableStateFlow<ProfileEntity?>(profileA)
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns activeFlow

        val repo = RecentViewsRepository(
            recentViewDao = dao,
            profileRepository = profileRepo,
            clock = TestClock()
        )

        repo.recentForActiveProfile.test {
            val first = awaitItem()
            assertThat(first.map { it.countryCode }).containsExactly("DEU")

            // Переключаем активный профиль → flatMapLatest перестроится.
            activeFlow.value = profileB
            val second = awaitItem()
            assertThat(second.map { it.countryCode }).containsExactly("USA")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll removes only active profile rows`() = runTest {
        val dao = FakeRecentViewDao()
        dao.upsert(com.example.hm_third_count.data.local.RecentViewEntity("DEU", profileA.id, 1L))
        dao.upsert(com.example.hm_third_count.data.local.RecentViewEntity("USA", profileB.id, 2L))

        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(profileA)
        val repo = RecentViewsRepository(dao, profileRepo, TestClock())

        repo.clearAll()
        assertThat(dao.snapshot().map { it.profileId }).containsExactly(profileB.id)
    }
}
