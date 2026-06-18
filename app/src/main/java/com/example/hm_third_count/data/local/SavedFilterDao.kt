package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {

    @Query("SELECT * FROM saved_filters WHERE profileId = :profileId ORDER BY createdAt ASC")
    fun observeByProfile(profileId: Long): Flow<List<SavedFilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedFilterEntity): Long

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM saved_filters WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
