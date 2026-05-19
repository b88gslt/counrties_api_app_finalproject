package com.example.hm_third_count.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.local.AreaUnit
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.ProfileEntity
import com.example.hm_third_count.data.local.ThemeMode
import com.example.hm_third_count.data.repository.ProfileRepository
import com.example.hm_third_count.sync.SyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileView(
    val id: Long,
    val name: String,
    val colorHex: String
)

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val areaUnit: AreaUnit = AreaUnit.KM2,
    val showVisitedBadge: Boolean = true,
    val cacheTtlHours: Int = 24,
    val backgroundRefreshHours: Int = 12,
    val lastSyncAt: Long = 0L,
    val syncRunning: Boolean = false,
    // ----- Profiles -----
    val profiles: List<ProfileView> = emptyList(),
    val activeProfileId: Long? = null
)

sealed class SettingsEvent {
    data class SetTheme(val mode: ThemeMode) : SettingsEvent()
    data class SetAreaUnit(val unit: AreaUnit) : SettingsEvent()
    data class SetShowVisitedBadge(val show: Boolean) : SettingsEvent()
    data class SetCacheTtl(val hours: Int) : SettingsEvent()
    data class SetBackgroundRefresh(val hours: Int) : SettingsEvent()
    data object SyncNow : SettingsEvent()
    data object DownloadAllCountries : SettingsEvent()
    // ----- Profiles -----
    data class SwitchProfile(val id: Long) : SettingsEvent()
    data class CreateProfile(val name: String, val colorHex: String) : SettingsEvent()
    data class RenameProfile(val id: Long, val name: String) : SettingsEvent()
    data class DeleteProfile(val id: Long) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: CountriesPreferences,
    private val profileRepository: ProfileRepository,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    /**
     * combine 4 источников:
     *  - настройки из DataStore (preferences.settings)
     *  - индикатор фоновой работы (syncCoordinator.syncRunning)
     *  - список профилей (profileRepository.profiles)
     *  - активный профиль (profileRepository.activeProfile)
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.settings,
        syncCoordinator.syncRunning,
        profileRepository.profiles,
        profileRepository.activeProfile
    ) { s, running, profiles, active ->
        SettingsUiState(
            theme = s.themeMode,
            areaUnit = s.areaUnit,
            showVisitedBadge = s.showVisitedBadge,
            cacheTtlHours = s.cacheTtlHours,
            backgroundRefreshHours = s.backgroundRefreshHours,
            lastSyncAt = s.lastFullSyncAt,
            syncRunning = running,
            profiles = profiles.map { it.toView() },
            activeProfileId = active?.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetTheme -> viewModelScope.launch { preferences.setThemeMode(event.mode) }
            is SettingsEvent.SetAreaUnit -> viewModelScope.launch { preferences.setAreaUnit(event.unit) }
            is SettingsEvent.SetShowVisitedBadge -> viewModelScope.launch {
                preferences.setShowVisitedBadge(event.show)
            }
            is SettingsEvent.SetCacheTtl -> viewModelScope.launch {
                preferences.setCacheTtlHours(event.hours)
            }
            is SettingsEvent.SetBackgroundRefresh -> viewModelScope.launch {
                preferences.setBackgroundRefreshHours(event.hours)
                syncCoordinator.rescheduleByPreference()
            }
            SettingsEvent.SyncNow -> syncCoordinator.triggerManualSync()
            SettingsEvent.DownloadAllCountries -> syncCoordinator.enqueueWarmup()
            is SettingsEvent.SwitchProfile -> viewModelScope.launch {
                profileRepository.setActive(event.id)
            }
            is SettingsEvent.CreateProfile -> viewModelScope.launch {
                if (event.name.isNotBlank()) {
                    profileRepository.create(event.name, event.colorHex)
                }
            }
            is SettingsEvent.RenameProfile -> viewModelScope.launch {
                if (event.name.isNotBlank()) profileRepository.rename(event.id, event.name)
            }
            is SettingsEvent.DeleteProfile -> viewModelScope.launch {
                profileRepository.delete(event.id)
            }
        }
    }

    private fun ProfileEntity.toView() = ProfileView(id, name, colorHex)
}
