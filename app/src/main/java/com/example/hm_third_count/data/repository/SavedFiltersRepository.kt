package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.SavedFilterDao
import com.example.hm_third_count.data.local.SavedFilterEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Пресеты фильтров — реактивно завязаны на активный профиль.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SavedFiltersRepository @Inject constructor(
    private val savedFilterDao: SavedFilterDao,
    private val profileRepository: ProfileRepository,
    private val clock: Clock
) {

    val savedFilters: Flow<List<SavedFilterEntity>> =
        profileRepository.activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(emptyList()) else savedFilterDao.observeByProfile(p.id)
        }

    suspend fun save(name: String, region: String, showFavoritesOnly: Boolean): Long {
        val profileId = profileRepository.activeProfile.first()?.id ?: return -1L
        return savedFilterDao.insert(
            SavedFilterEntity(
                profileId = profileId,
                name = name.trim(),
                region = region,
                showFavoritesOnly = showFavoritesOnly,
                createdAt = clock.nowMillis()
            )
        )
    }

    suspend fun delete(id: Long) = savedFilterDao.delete(id)
}
