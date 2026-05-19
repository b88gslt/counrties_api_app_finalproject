package com.example.hm_third_count.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hm_third_count.data.repository.ProfileRepository
import com.example.hm_third_count.presentation.settings.ProfileView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSwitcherUiState(
    val profiles: List<ProfileView> = emptyList(),
    val activeProfileId: Long? = null
) {
    val activeProfile: ProfileView?
        get() = profiles.firstOrNull { it.id == activeProfileId }
}

@HiltViewModel
class ProfileSwitcherViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileSwitcherUiState> = combine(
        profileRepository.profiles,
        profileRepository.activeProfile
    ) { profiles, active ->
        ProfileSwitcherUiState(
            profiles = profiles.map { ProfileView(it.id, it.name, it.colorHex) },
            activeProfileId = active?.id
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileSwitcherUiState())

    fun switch(id: Long) = viewModelScope.launch { profileRepository.setActive(id) }
    fun create(name: String, colorHex: String) = viewModelScope.launch {
        if (name.isNotBlank()) profileRepository.create(name, colorHex)
    }
    fun rename(id: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) profileRepository.rename(id, name)
    }
    fun delete(id: Long) = viewModelScope.launch { profileRepository.delete(id) }
}
