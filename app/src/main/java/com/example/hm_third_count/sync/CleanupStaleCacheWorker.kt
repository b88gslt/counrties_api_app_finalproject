package com.example.hm_third_count.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hm_third_count.data.local.CollectionItemDao
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.RecentViewDao
import com.example.hm_third_count.data.local.VisitDao
import com.example.hm_third_count.data.local.WishlistDao
import com.example.hm_third_count.data.repository.Clock
import com.example.hm_third_count.data.repository.ProfileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Чистка устаревших записей кэша. Сохраняем только страны, которые «зацеплены»
 * пользовательскими данными (visited, wishlist, collections, recent_views) —
 * остальное удаляем, если оно старше cleanupThreshold.
 */
@HiltWorker
class CleanupStaleCacheWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val countryCacheDao: CountryCacheDao,
    private val profileRepository: ProfileRepository,
    private val visitDao: VisitDao,
    private val wishlistDao: WishlistDao,
    private val collectionItemDao: CollectionItemDao,
    private val recentViewDao: RecentViewDao,
    private val clock: Clock
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val profile = profileRepository.activeProfile.first() ?: return Result.success()

        val pinned = buildSet {
            visitDao.observeByProfile(profile.id).first().forEach { add(it.countryCode) }
            wishlistDao.observeByProfile(profile.id).first().forEach { add(it.countryCode) }
            collectionItemDao.observeAllForProfile(profile.id).first()
                .forEach { add(it.countryCode) }
            recentViewDao.observeByProfile(profile.id).first().forEach { add(it.countryCode) }
        }

        val threshold = clock.nowMillis() - CLEANUP_AGE_MS
        val all = countryCacheDao.observeAll().first()
        all.filter { it.fetchedAt < threshold && it.countryCode !in pinned }
            .forEach { countryCacheDao.deleteByCode(it.countryCode) }

        Result.success()
    }.getOrDefault(Result.failure())

    companion object {
        /** 14 дней — кэш долгоживущий, но не вечный. */
        private const val CLEANUP_AGE_MS = 14L * 24L * 60L * 60L * 1000L
    }
}
