package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentViewDao {

    /** Все просмотры профиля от свежих к старым. */
    @Query("SELECT * FROM recent_views WHERE profileId = :profileId ORDER BY viewedAt DESC")
    fun observeByProfile(profileId: Long): Flow<List<RecentViewEntity>>

    /** Дедуплицирующий апсёрт: повторный INSERT по тому же PK перезаписывает viewedAt. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentViewEntity)

    @Query("DELETE FROM recent_views WHERE countryCode = :countryCode AND profileId = :profileId")
    suspend fun delete(countryCode: String, profileId: Long)

    @Query("DELETE FROM recent_views WHERE profileId = :profileId")
    suspend fun clearForProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM recent_views WHERE profileId = :profileId")
    suspend fun countForProfile(profileId: Long): Int
}
