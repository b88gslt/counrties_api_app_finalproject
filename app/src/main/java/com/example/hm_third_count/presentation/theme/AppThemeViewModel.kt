package com.example.hm_third_count.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Маленькая VM ровно для одной задачи — отдать MainActivity выбранный пользователем
 * режим темы. Так MainActivity не зависит от тяжёлой SettingsViewModel.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    preferences: CountriesPreferences
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.settings
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
