package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.RecentViewDao
import com.example.hm_third_count.data.local.RecentViewEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentViewsRepository @Inject constructor(
    private val recentViewDao: RecentViewDao,
    private val profileRepository: ProfileRepository,
    private val clock: Clock
) {

    /**
     * Поток истории просмотров активного профиля — реактивно перестраивается при переключении профиля.
     * Это и есть мост между разными профилями: меняется activeProfile → flatMapLatest на DAO с новым profileId.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentForActiveProfile: Flow<List<RecentViewEntity>> =
        profileRepository.activeProfile.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList())
            else recentViewDao.observeByProfile(profile.id)
        }

    /**
     * Записать просмотр страны. Дедупликация на уровне БД: PRIMARY KEY (countryCode, profileId) +
     * REPLACE в DAO — повторный вызов апдейтит viewedAt, не создаёт новую строку.
     */
    suspend fun recordView(countryCode: String) {
        val profile = profileRepository.activeProfile.first() ?: return
        recentViewDao.upsert(
            RecentViewEntity(
                countryCode = countryCode,
                profileId = profile.id,
                viewedAt = clock.nowMillis()
            )
        )
    }

    suspend fun delete(countryCode: String) {
        val profile = profileRepository.activeProfile.first() ?: return
        recentViewDao.delete(countryCode, profile.id)
    }

    suspend fun clearAll() {
        val profile = profileRepository.activeProfile.first() ?: return
        recentViewDao.clearForProfile(profile.id)
    }
}
