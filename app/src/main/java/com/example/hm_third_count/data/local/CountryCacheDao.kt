package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryCacheDao {

    @Query("SELECT * FROM country_cache WHERE countryCode = :code")
    fun observeByCode(code: String): Flow<CountryCacheEntity?>

    @Query("SELECT * FROM country_cache WHERE countryCode = :code")
    suspend fun getByCode(code: String): CountryCacheEntity?

    @Query("SELECT * FROM country_cache")
    fun observeAll(): Flow<List<CountryCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CountryCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CountryCacheEntity>)

    @Query("DELETE FROM country_cache WHERE countryCode = :code")
    suspend fun deleteByCode(code: String)

    @Query("DELETE FROM country_cache WHERE fetchedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
