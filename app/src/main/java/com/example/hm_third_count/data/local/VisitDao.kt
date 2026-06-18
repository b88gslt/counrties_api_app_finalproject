package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits WHERE profileId = :profileId ORDER BY visitedAt DESC")
    fun observeByProfile(profileId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE countryCode = :code AND profileId = :profileId")
    fun observeOne(code: String, profileId: Long): Flow<VisitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VisitEntity)

    @Query("DELETE FROM visits WHERE countryCode = :code AND profileId = :profileId")
    suspend fun delete(code: String, profileId: Long)

    @Query("DELETE FROM visits WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
