package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.CollectionDao
import com.example.hm_third_count.data.local.CollectionItemEntity
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.testutil.FakeCollectionItemDao
import com.example.hm_third_count.testutil.MainDispatcherRule
import com.example.hm_third_count.testutil.TestClock
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsRepositoryTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val profile = ProfileEntity(id = 1L, name = "A", colorHex = "#1", createdAt = 0L)

    private fun buildRepo(): Triple<CollectionsRepository, FakeCollectionItemDao, TestClock> {
        val itemDao = FakeCollectionItemDao()
        val collectionDao = mockk<CollectionDao>(relaxed = true)
        val profileRepo = mockk<ProfileRepository>()
        every { profileRepo.activeProfile } returns flowOf(profile)
        val clock = TestClock()
        val repo = CollectionsRepository(
            collectionDao = collectionDao,
            collectionItemDao = itemDao,
            profileRepository = profileRepo,
            clock = clock
        )
        return Triple(repo, itemDao, clock)
    }

    @Test
    fun `adding same country twice does not create duplicate`() = runTest {
        val (repo, itemDao, _) = buildRepo()
        repo.addCountry(collectionId = 10L, countryCode = "DEU")
        repo.addCountry(collectionId = 10L, countryCode = "DEU")
        // PRIMARY KEY (collectionId, countryCode) + REPLACE — ровно одна запись.
        assertThat(itemDao.snapshot()).hasSize(1)
    }

    @Test
    fun `removeCountry removes matching pair only`() = runTest {
        val (repo, itemDao, _) = buildRepo()
        repo.addCountry(10L, "DEU")
        repo.addCountry(10L, "USA")
        repo.addCountry(20L, "DEU")

        repo.removeCountry(10L, "DEU")

        val remaining = itemDao.snapshot().map { it.collectionId to it.countryCode }
        assertThat(remaining).containsExactly(10L to "USA", 20L to "DEU")
    }

    @Test
    fun `setCountryMembership adds missing and removes extra (diff)`() = runTest {
        val (repo, itemDao, _) = buildRepo()
        // Изначально страна состоит в коллекциях 1 и 2.
        itemDao.upsert(CollectionItemEntity(1L, "DEU", 0L))
        itemDao.upsert(CollectionItemEntity(2L, "DEU", 0L))

        // Пользователь сохранил выбор: только {2, 3}.
        repo.setCountryMembership("DEU", selectedIds = setOf(2L, 3L))

        val byCollection = itemDao.snapshot()
            .filter { it.countryCode == "DEU" }
            .map { it.collectionId }
            .toSet()

        // 1 удалили, 2 оставили, 3 добавили.
        assertThat(byCollection).containsExactly(2L, 3L)
    }

    @Test
    fun `setCountryMembership with empty selection clears all memberships of this country`() = runTest {
        val (repo, itemDao, _) = buildRepo()
        itemDao.upsert(CollectionItemEntity(1L, "DEU", 0L))
        itemDao.upsert(CollectionItemEntity(2L, "DEU", 0L))

        repo.setCountryMembership("DEU", selectedIds = emptySet())

        assertThat(itemDao.snapshot().filter { it.countryCode == "DEU" }).isEmpty()
    }

    @Test
    fun `setCountryMembership preserves memberships of other countries`() = runTest {
        val (repo, itemDao, _) = buildRepo()
        itemDao.upsert(CollectionItemEntity(1L, "DEU", 0L))
        itemDao.upsert(CollectionItemEntity(1L, "USA", 0L))

        repo.setCountryMembership("DEU", selectedIds = emptySet())

        // USA в коллекции 1 не должен пострадать.
        assertThat(itemDao.snapshot().map { it.countryCode to it.collectionId })
            .containsExactly("USA" to 1L)
    }
}
