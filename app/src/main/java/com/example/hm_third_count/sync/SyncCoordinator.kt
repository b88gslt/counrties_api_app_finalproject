package com.example.hm_third_count.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.CountryCacheDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Точка управления фоновыми задачами:
 *  - bootstrap() — вызвать при старте приложения: запланировать periodic в зависимости от настроек,
 *    а если кэш пустой — сразу прогреть страны.
 *  - rescheduleByPreference() — пользователь поменял интервал в настройках → перепланировать.
 *  - triggerManualSync() — кнопка "Sync now" в настройках.
 *  - syncRunning — Flow<Boolean>, идёт ли сейчас фоновое обновление (для баннера в UI).
 */
@Singleton
class SyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: CountriesPreferences,
    private val countryCacheDao: CountryCacheDao
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /**
     * Идёт ли любое наше фоновое обновление прямо сейчас.
     * Считаем только RUNNING; ENQUEUED — это «ожидает следующего тика» для periodic,
     * это нормальное состояние и не значит что прямо сейчас что-то делается.
     */
    val syncRunning: Flow<Boolean> = combine(
        workManager.getWorkInfosByTagFlow(TAG_REFRESH),
        workManager.getWorkInfosByTagFlow(TAG_WARMUP)
    ) { refreshList, warmupList ->
        (refreshList + warmupList).any { it.state == WorkInfo.State.RUNNING }
    }

    /** Последний успешный sync (epoch millis) — для отображения "Last sync: Xh ago". */
    val lastSyncAt: Flow<Long> = preferences.settings.map { it.lastFullSyncAt }

    suspend fun bootstrap() {
        // Если кэш пустой — запускаем warmup сразу же при старте.
        val cacheEmpty = countryCacheDao.observeAll().first().isEmpty()
        if (cacheEmpty) {
            enqueueWarmup()
        }
        rescheduleByPreference()
        scheduleCleanup()
    }

    suspend fun rescheduleByPreference() {
        val hours = preferences.settings.first().backgroundRefreshHours
        if (hours <= 0) {
            workManager.cancelUniqueWork(WORK_REFRESH)
            return
        }
        val request = PeriodicWorkRequestBuilder<RefreshCacheWorker>(
            hours.toLong(), TimeUnit.HOURS
        )
            .addTag(TAG_REFRESH)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_REFRESH,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun triggerManualSync() {
        val request = OneTimeWorkRequestBuilder<RefreshCacheWorker>()
            .addTag(TAG_REFRESH)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(WORK_MANUAL_SYNC, ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueWarmup() {
        val request = OneTimeWorkRequestBuilder<WarmupAllCountriesWorker>()
            .addTag(TAG_WARMUP)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(WORK_WARMUP, ExistingWorkPolicy.KEEP, request)
    }

    private fun scheduleCleanup() {
        val request = PeriodicWorkRequestBuilder<CleanupStaleCacheWorker>(
            7, TimeUnit.DAYS
        )
            .addTag(TAG_CLEANUP)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_CLEANUP,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val WORK_REFRESH = "refresh_cache_periodic"
        private const val WORK_WARMUP = "warmup_all_countries"
        private const val WORK_MANUAL_SYNC = "manual_sync"
        private const val WORK_CLEANUP = "cleanup_stale_cache"
        const val TAG_REFRESH = "tag:refresh"
        const val TAG_WARMUP = "tag:warmup"
        const val TAG_CLEANUP = "tag:cleanup"
    }
}
