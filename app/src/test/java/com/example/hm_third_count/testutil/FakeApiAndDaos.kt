package com.example.hm_third_count.testutil

import com.example.hm_third_count.data.api.CountriesApi
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.CountryCacheEntity
import com.example.hm_third_count.data.local.FavoriteDao
import com.example.hm_third_count.data.local.FavoriteEntity
import com.example.hm_third_count.data.model.Country
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Управляемый Fake API: можно задать ответ на каждый вызов или эмулировать ошибку сети.
 */
class FakeCountriesApi : CountriesApi {
    var nextResponse: ApiResponse = ApiResponse.NetworkError
    var callCount: Int = 0
        private set

    sealed class ApiResponse {
        data class Success(val countries: List<Country>) : ApiResponse()
        data object NetworkError : ApiResponse()
    }

    override suspend fun getAllCountries(): List<Country> = handle()
    override suspend fun searchCountriesByName(name: String): List<Country> =
        handle().filter { it.name.common.contains(name, ignoreCase = true) }
    override suspend fun getCountryByCode(code: String): List<Country> =
        handle().filter { it.code == code }
    override suspend fun getCountriesByRegion(region: String): List<Country> =
        handle().filter { it.region.equals(region, ignoreCase = true) }

    private fun handle(): List<Country> {
        callCount++
        return when (val r = nextResponse) {
            is ApiResponse.Success -> r.countries
            ApiResponse.NetworkError -> throw RuntimeException("Unable to resolve host")
        }
    }
}

class FakeCountryCacheDao : CountryCacheDao {
    private val data = MutableStateFlow<Map<String, CountryCacheEntity>>(emptyMap())

    override fun observeByCode(code: String): Flow<CountryCacheEntity?> =
        data.map { it[code] }

    override suspend fun getByCode(code: String): CountryCacheEntity? = data.value[code]

    override fun observeAll(): Flow<List<CountryCacheEntity>> =
        data.map { it.values.toList() }

    override suspend fun upsert(entity: CountryCacheEntity) {
        data.value = data.value + (entity.countryCode to entity)
    }

    override suspend fun upsertAll(entities: List<CountryCacheEntity>) {
        data.value = data.value + entities.associateBy { it.countryCode }
    }

    override suspend fun deleteByCode(code: String) {
        data.value = data.value - code
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        data.value = data.value.filterValues { it.fetchedAt >= threshold }
    }

    fun snapshot(): List<CountryCacheEntity> = data.value.values.toList()
}

class FakeFavoriteDao : FavoriteDao {
    private val data = MutableStateFlow<Map<Pair<String, Long>, FavoriteEntity>>(emptyMap())

    override fun observeByProfile(profileId: Long): Flow<List<FavoriteEntity>> =
        data.map { map -> map.values.filter { it.profileId == profileId } }

    override suspend fun insert(entity: FavoriteEntity) {
        val key = entity.countryCode to entity.profileId
        data.value = data.value + (key to entity)
    }

    override suspend fun delete(code: String, profileId: Long) {
        data.value = data.value - (code to profileId)
    }

    override suspend fun isFavorite(code: String, profileId: Long): Boolean =
        data.value.containsKey(code to profileId)

    fun snapshot(): List<FavoriteEntity> = data.value.values.toList()
}
