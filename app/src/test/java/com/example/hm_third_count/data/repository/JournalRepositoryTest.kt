package com.example.hm_third_count.data.repository

import app.cash.turbine.test
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.testutil.FakeCountryNoteDao
import com.example.hm_third_count.testutil.FakeVisitDao
import com.example.hm_third_count.testutil.FakeWishlistDao
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
class JournalRepositoryTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val profile = ProfileEntity(id = 1L, name = "A", colorHex = "#1", createdAt = 0L)

    private fun buildRepo(
        activeProfile: ProfileEntity? = profile,
        clock: TestClock = TestClock()
    ): JournalContext {
        val visitDao = FakeVisitDao()
        val wishlistDao = FakeWishlistDao()
        val noteDao = FakeCountryNoteDao()
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(activeProfile)
        return JournalContext(
            repo = JournalRepository(visitDao, wishlistDao, noteDao, profileRepo, clock),
            visitDao = visitDao,
            wishlistDao = wishlistDao,
            noteDao = noteDao
        )
    }

    private data class JournalContext(
        val repo: JournalRepository,
        val visitDao: FakeVisitDao,
        val wishlistDao: FakeWishlistDao,
        val noteDao: FakeCountryNoteDao
    )

    @Test
    fun `upsertVisit twice for same country does not create duplicate`() = runTest {
        val ctx = buildRepo()
        ctx.repo.upsertVisit("DEU", visitedAt = 100L, rating = 5, note = "First")
        ctx.repo.upsertVisit("DEU", visitedAt = 200L, rating = 3, note = "Updated")
        // PRIMARY KEY (countryCode, profileId) — ровно одна запись.
        assertThat(ctx.visitDao.snapshot()).hasSize(1)
        val only = ctx.visitDao.snapshot().first()
        // И это обновлённая запись.
        assertThat(only.rating).isEqualTo(3)
        assertThat(only.note).isEqualTo("Updated")
    }

    @Test
    fun `upsertVisit clamps rating to 1-5`() = runTest {
        val ctx = buildRepo()
        ctx.repo.upsertVisit("DEU", visitedAt = 100L, rating = 99, note = null)
        assertThat(ctx.visitDao.snapshot().first().rating).isEqualTo(5)

        ctx.repo.upsertVisit("USA", visitedAt = 100L, rating = -5, note = null)
        val usa = ctx.visitDao.snapshot().first { it.countryCode == "USA" }
        assertThat(usa.rating).isEqualTo(1)
    }

    @Test
    fun `upsertNote with blank text deletes the note`() = runTest {
        val ctx = buildRepo()
        ctx.repo.upsertNote("DEU", "First note")
        ctx.repo.observeNote("DEU").test {
            assertThat(awaitItem()?.text).isEqualTo("First note")
            // Передаём пустую строку — должна сработать как delete.
            ctx.repo.upsertNote("DEU", "   ")
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `upsertNote trims whitespace`() = runTest {
        val ctx = buildRepo()
        ctx.repo.upsertNote("DEU", "  hello  ")
        ctx.repo.observeNote("DEU").test {
            assertThat(awaitItem()?.text).isEqualTo("hello")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `visits flow switches when active profile changes`() = runTest {
        val profileA = ProfileEntity(1L, "A", "#1", 0L)
        val profileB = ProfileEntity(2L, "B", "#2", 0L)
        val activeFlow = MutableStateFlow<ProfileEntity?>(profileA)
        val visitDao = FakeVisitDao()
        // Заранее кладём данные для обоих профилей.
        visitDao.upsert(
            com.example.hm_third_count.data.local.VisitEntity("DEU", 1L, 100L, 5, null, 0L)
        )
        visitDao.upsert(
            com.example.hm_third_count.data.local.VisitEntity("USA", 2L, 100L, 4, null, 0L)
        )

        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns activeFlow
        val repo = JournalRepository(
            visitDao, FakeWishlistDao(), FakeCountryNoteDao(), profileRepo, TestClock()
        )

        repo.visits.test {
            assertThat(awaitItem().map { it.countryCode }).containsExactly("DEU")
            activeFlow.value = profileB
            assertThat(awaitItem().map { it.countryCode }).containsExactly("USA")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
