package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE profileId = :profileId")
    fun observeByProfile(profileId: Long): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE countryCode = :code AND profileId = :profileId")
    suspend fun delete(code: String, profileId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE countryCode = :code AND profileId = :profileId)")
    suspend fun isFavorite(code: String, profileId: Long): Boolean

    @Query("DELETE FROM favorites WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
