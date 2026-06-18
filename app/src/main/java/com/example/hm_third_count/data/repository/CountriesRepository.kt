package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.api.CountriesApi
import com.example.hm_third_count.data.local.CountriesPreferences
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.CountryCacheEntity
import com.example.hm_third_count.data.local.FavoriteDao
import com.example.hm_third_count.data.local.FavoriteEntity
import com.example.hm_third_count.data.model.Country
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CountriesRepository @Inject constructor(
    private val api: CountriesApi,
    private val favoriteDao: FavoriteDao,
    private val countryCacheDao: CountryCacheDao,
    private val json: Json,
    private val preferences: CountriesPreferences,
    private val profileRepository: ProfileRepository,
    private val clock: Clock
) {
    val sortCountriesAz: Flow<Boolean> = preferences.sortCountriesAz

    suspend fun setSortCountriesAz(enabled: Boolean) = preferences.setSortCountriesAz(enabled)

    /**
     * Избранное активного профиля одним потоком Room.
     * flatMapLatest перестраивает подписку при переключении профиля.
     */
    val favoritesBundle: Flow<FavoritesBundle> =
        profileRepository.activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(FavoritesBundle.EMPTY)
            else favoriteDao.observeByProfile(p.id).map { list ->
                FavoritesBundle(
                    codes = list.map { it.countryCode }.toSet(),
                    snapshots = list.mapNotNull { entity ->
                        entity.countrySnapshotJson?.let { raw ->
                            runCatching { json.decodeFromString<Country>(raw) }.getOrNull()
                        }
                    }
                )
            }
        }

    /** @see favoritesBundle */
    val favorites: Flow<Set<String>> = favoritesBundle.map { it.codes }

    /** @see favoritesBundle */
    val favoriteCountriesFromRoom: Flow<List<Country>> = favoritesBundle.map { it.snapshots }

    // ---- API calls ----

    suspend fun getAllCountries(): Result<List<Country>> = runCatching {
        try {
            val countries = api.getAllCountries()
            writeToCache(countries)
            countries
        } catch (e: Exception) {
            val cached = readAllFromCache()
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    suspend fun searchCountries(query: String): Result<List<Country>> = runCatching {
        try {
            val countries = if (query.isBlank()) api.getAllCountries()
            else api.searchCountriesByName(query)
            writeToCache(countries)
            countries
        } catch (e: Exception) {
            val cached = readAllFromCache()
            if (cached.isEmpty()) throw e
            if (query.isBlank()) cached
            else cached.filter { it.name.common.contains(query, ignoreCase = true) }
        }
    }

    suspend fun getCountryByCode(code: String): Result<Country?> = runCatching {
        val country = api.getCountryByCode(code).firstOrNull()
        if (country != null) writeToCache(listOf(country))
        country
    }

    suspend fun getCountriesByRegion(region: String): Result<List<Country>> = runCatching {
        try {
            val countries = api.getCountriesByRegion(region)
            writeToCache(countries)
            countries
        } catch (e: Exception) {
            val cached = readAllFromCache()
            if (cached.isEmpty()) throw e
            cached.filter { it.region.equals(region, ignoreCase = true) }
        }
    }

    private suspend fun readAllFromCache(): List<Country> =
        countryCacheDao.observeAll().first().mapNotNull { entity ->
            runCatching { json.decodeFromString<Country>(entity.json) }.getOrNull()
        }

    fun observeCachedCountry(code: String): Flow<CountryCacheEntity?> =
        countryCacheDao.observeByCode(code)

    fun decodeCachedCountry(entity: CountryCacheEntity): Country? =
        runCatching { json.decodeFromString<Country>(entity.json) }.getOrNull()

    private suspend fun writeToCache(countries: List<Country>) {
        if (countries.isEmpty()) return
        val now = clock.nowMillis()
        val entities = countries.map { country ->
            CountryCacheEntity(
                countryCode = country.code,
                json = json.encodeToString(Country.serializer(), country),
                fetchedAt = now
            )
        }
        countryCacheDao.upsertAll(entities)
    }

    // ---- Favorites (profile-aware) ----

    suspend fun addToFavorites(country: Country) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        val snapshot = json.encodeToString(Country.serializer(), country)
        favoriteDao.insert(
            FavoriteEntity(
                countryCode = country.code,
                profileId = profileId,
                countrySnapshotJson = snapshot
            )
        )
    }

    suspend fun removeFromFavorites(countryCode: String) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        favoriteDao.delete(countryCode, profileId)
    }

    suspend fun isFavorite(countryCode: String): Boolean {
        val profileId = profileRepository.activeProfile.first()?.id ?: return false
        return favoriteDao.isFavorite(countryCode, profileId)
    }
}

data class FavoritesBundle(
    val codes: Set<String>,
    val snapshots: List<Country>
) {
    companion object {
        val EMPTY = FavoritesBundle(emptySet(), emptyList())
    }
}
