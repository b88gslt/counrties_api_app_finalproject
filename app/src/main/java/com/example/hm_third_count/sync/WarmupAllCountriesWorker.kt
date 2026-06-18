package com.example.hm_third_count.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.repository.Clock
import com.example.hm_third_count.data.repository.CountriesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-shot предзагрузка ВСЕХ стран в кэш — для полноценного offline-first.
 * Запускается при первом запуске приложения и вручную из настроек.
 */
@HiltWorker
class WarmupAllCountriesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val countriesRepository: CountriesRepository,
    private val preferences: CountriesPreferences,
    private val clock: Clock
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val result = countriesRepository.getAllCountries()
        return if (result.isSuccess) {
            preferences.setLastFullSyncAt(clock.nowMillis())
            Result.success()
        } else {
            Result.retry()
        }
    }
}
