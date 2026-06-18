package com.example.hm_third_count.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hm_third_count.data.local.CollectionItemDao
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.VisitDao
import com.example.hm_third_count.data.local.WishlistDao
import com.example.hm_third_count.data.repository.Clock
import com.example.hm_third_count.data.repository.CountriesRepository
import com.example.hm_third_count.data.repository.ProfileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Периодическое обновление кэша для «дорогих» стран пользователя:
 * посещённые + wishlist + добавленные в коллекции. Дороже всего без сети потерять именно их.
 */
@HiltWorker
class RefreshCacheWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val profileRepository: ProfileRepository,
    private val visitDao: VisitDao,
    private val wishlistDao: WishlistDao,
    private val collectionItemDao: CollectionItemDao,
    private val countriesRepository: CountriesRepository,
    private val preferences: CountriesPreferences,
    private val clock: Clock
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val profile = profileRepository.activeProfile.first() ?: return Result.success()

        val pinned = buildSet {
            visitDao.observeByProfile(profile.id).first().forEach { add(it.countryCode) }
            wishlistDao.observeByProfile(profile.id).first().forEach { add(it.countryCode) }
            collectionItemDao.observeAllForProfile(profile.id).first()
                .forEach { add(it.countryCode) }
        }

        if (pinned.isEmpty()) {
            preferences.setLastFullSyncAt(clock.nowMillis())
            return Result.success()
        }

        var hadFailure = false
        for (code in pinned) {
            // Репозиторий сам пишет результат в country_cache при успехе.
            countriesRepository.getCountryByCode(code).onFailure { hadFailure = true }
        }

        preferences.setLastFullSyncAt(clock.nowMillis())
        return if (hadFailure) Result.retry() else Result.success()
    }
}
