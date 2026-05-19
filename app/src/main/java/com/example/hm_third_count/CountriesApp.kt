package com.example.hm_third_count

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.hm_third_count.data.repository.ProfileRepository
import com.example.hm_third_count.sync.SyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CountriesApp : Application(), Configuration.Provider {

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncCoordinator: SyncCoordinator

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // Создаём профиль "Default" при первом запуске.
            profileRepository.ensureDefaultProfile()
            // Планируем фоновые задачи согласно настройкам пользователя.
            syncCoordinator.bootstrap()
        }
    }
}
