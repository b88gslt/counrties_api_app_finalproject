package com.example.hm_third_count.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.countriesPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "countries_preferences"
)

private val KEY_SORT_AZ = booleanPreferencesKey("sort_countries_az")
private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
private val KEY_AREA_UNIT = stringPreferencesKey("area_unit")
private val KEY_SHOW_VISITED_BADGE = booleanPreferencesKey("show_visited_badge")
private val KEY_CACHE_TTL_HOURS = intPreferencesKey("cache_ttl_hours")
private val KEY_BACKGROUND_REFRESH_HOURS = intPreferencesKey("background_refresh_hours")
private val KEY_ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
private val KEY_LAST_FULL_SYNC_AT = longPreferencesKey("last_full_sync_at")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AreaUnit { KM2, MI2 }

data class AppSettings(
    val sortAz: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val areaUnit: AreaUnit = AreaUnit.KM2,
    val showVisitedBadge: Boolean = true,
    val cacheTtlHours: Int = 24,
    val backgroundRefreshHours: Int = 12,
    val lastFullSyncAt: Long = 0L
)

@Singleton
class CountriesPreferences @Inject constructor(
    @ApplicationContext private val app: Context
) {
    private val dataStore = app.countriesPreferencesDataStore

    /** Сортировка списка по имени (A–Z). */
    val sortCountriesAz: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SORT_AZ] ?: false
    }

    /** Все настройки одним пакетом — удобно для экрана Settings и для combine. */
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            sortAz = prefs[KEY_SORT_AZ] ?: false,
            themeMode = prefs[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            areaUnit = prefs[KEY_AREA_UNIT]?.let { runCatching { AreaUnit.valueOf(it) }.getOrNull() }
                ?: AreaUnit.KM2,
            showVisitedBadge = prefs[KEY_SHOW_VISITED_BADGE] ?: true,
            cacheTtlHours = prefs[KEY_CACHE_TTL_HOURS] ?: 24,
            backgroundRefreshHours = prefs[KEY_BACKGROUND_REFRESH_HOURS] ?: 12,
            lastFullSyncAt = prefs[KEY_LAST_FULL_SYNC_AT] ?: 0L
        )
    }

    /** Активный профиль. null означает «профилей ещё нет» (первый запуск). */
    val activeProfileId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID]
    }

    suspend fun setSortCountriesAz(enabled: Boolean) {
        dataStore.edit { it[KEY_SORT_AZ] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setAreaUnit(unit: AreaUnit) {
        dataStore.edit { it[KEY_AREA_UNIT] = unit.name }
    }

    suspend fun setShowVisitedBadge(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_VISITED_BADGE] = enabled }
    }

    suspend fun setCacheTtlHours(hours: Int) {
        dataStore.edit { it[KEY_CACHE_TTL_HOURS] = hours }
    }

    suspend fun setBackgroundRefreshHours(hours: Int) {
        dataStore.edit { it[KEY_BACKGROUND_REFRESH_HOURS] = hours }
    }

    suspend fun setActiveProfileId(id: Long) {
        dataStore.edit { it[KEY_ACTIVE_PROFILE_ID] = id }
    }

    suspend fun setLastFullSyncAt(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_FULL_SYNC_AT] = timestamp }
    }
}
