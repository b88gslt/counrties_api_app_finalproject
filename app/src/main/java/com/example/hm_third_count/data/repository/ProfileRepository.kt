package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.ProfileDao
import com.example.hm_third_count.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val preferences: CountriesPreferences
) {
    val profiles: Flow<List<ProfileEntity>> = profileDao.observeAll()

    /**
     * Активный профиль с фолбэком на «первый по дате» — нужно на случай,
     * когда профиль из DataStore был удалён.
     */
    val activeProfile: Flow<ProfileEntity?> = combine(
        preferences.activeProfileId,
        profileDao.observeAll()
    ) { activeId, all ->
        when {
            all.isEmpty() -> null
            activeId == null -> all.first()
            else -> all.firstOrNull { it.id == activeId } ?: all.first()
        }
    }.distinctUntilChanged()

    suspend fun ensureDefaultProfile(): Long {
        val count = profileDao.count()
        if (count > 0) {
            // Уже что-то есть — синхронизируем activeProfileId если он пустой.
            val activeId = preferences.activeProfileId.first()
            if (activeId == null) {
                val first = profileDao.observeAll().first().first()
                preferences.setActiveProfileId(first.id)
                return first.id
            }
            return activeId
        }
        val newId = profileDao.insert(
            ProfileEntity(
                name = "Default",
                colorHex = "#4FC3F7",
                createdAt = System.currentTimeMillis()
            )
        )
        preferences.setActiveProfileId(newId)
        return newId
    }

    suspend fun create(name: String, colorHex: String): Long {
        val id = profileDao.insert(
            ProfileEntity(
                name = name,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis()
            )
        )
        preferences.setActiveProfileId(id)
        return id
    }

    suspend fun rename(id: Long, name: String) {
        val entity = profileDao.getById(id) ?: return
        profileDao.update(entity.copy(name = name))
    }

    suspend fun setActive(id: Long) {
        preferences.setActiveProfileId(id)
    }

    suspend fun delete(id: Long) {
        profileDao.deleteById(id)
        val remaining = profileDao.observeAll().first()
        if (remaining.isNotEmpty() && preferences.activeProfileId.first() == id) {
            preferences.setActiveProfileId(remaining.first().id)
        }
    }
}
