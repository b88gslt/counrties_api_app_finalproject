package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.CollectionDao
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.CountryNoteDao
import com.example.hm_third_count.data.local.FavoriteDao
import com.example.hm_third_count.data.local.ProfileDao
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.data.local.RecentViewDao
import com.example.hm_third_count.data.local.SavedFilterDao
import com.example.hm_third_count.data.local.VisitDao
import com.example.hm_third_count.data.local.WishlistDao
import com.example.hm_third_count.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val profileDao = mockk<ProfileDao>(relaxed = true)
    private val preferences = mockk<CountriesPreferences>(relaxed = true)
    private val visitDao = mockk<VisitDao>(relaxed = true)
    private val wishlistDao = mockk<WishlistDao>(relaxed = true)
    private val countryNoteDao = mockk<CountryNoteDao>(relaxed = true)
    private val favoriteDao = mockk<FavoriteDao>(relaxed = true)
    private val savedFilterDao = mockk<SavedFilterDao>(relaxed = true)
    private val recentViewDao = mockk<RecentViewDao>(relaxed = true)
    private val collectionDao = mockk<CollectionDao>(relaxed = true)

    private val profilesFlow = MutableStateFlow(
        listOf(
            ProfileEntity(1L, "A", "#1", 0L),
            ProfileEntity(2L, "B", "#2", 1L)
        )
    )

    private fun repo() = ProfileRepository(
        profileDao = profileDao,
        preferences = preferences,
        visitDao = visitDao,
        wishlistDao = wishlistDao,
        countryNoteDao = countryNoteDao,
        favoriteDao = favoriteDao,
        savedFilterDao = savedFilterDao,
        recentViewDao = recentViewDao,
        collectionDao = collectionDao
    )

    @Test
    fun `delete removes profile data before profile row`() = runTest {
        every { profileDao.observeAll() } returns profilesFlow
        coEvery { preferences.activeProfileId } returns flowOf(1L)

        repo().delete(1L)

        coVerifyOrder {
            collectionDao.deleteByProfile(1L)
            visitDao.deleteByProfile(1L)
            wishlistDao.deleteByProfile(1L)
            countryNoteDao.deleteByProfile(1L)
            favoriteDao.deleteByProfile(1L)
            savedFilterDao.deleteByProfile(1L)
            recentViewDao.clearForProfile(1L)
            profileDao.deleteById(1L)
        }
    }

    @Test
    fun `delete switches active profile when deleted profile was active`() = runTest {
        val remaining = listOf(ProfileEntity(2L, "B", "#2", 1L))
        every { profileDao.observeAll() } returnsMany listOf(
            flowOf(profilesFlow.value),
            flowOf(remaining)
        )
        coEvery { preferences.activeProfileId } returns flowOf(1L)

        repo().delete(1L)

        coVerify { preferences.setActiveProfileId(2L) }
    }
}
