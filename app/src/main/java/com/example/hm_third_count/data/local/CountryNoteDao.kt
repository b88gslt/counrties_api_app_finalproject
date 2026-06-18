package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryNoteDao {

    @Query("SELECT * FROM country_notes WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeByProfile(profileId: Long): Flow<List<CountryNoteEntity>>

    @Query("SELECT * FROM country_notes WHERE countryCode = :code AND profileId = :profileId")
    fun observeOne(code: String, profileId: Long): Flow<CountryNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CountryNoteEntity)

    @Query("DELETE FROM country_notes WHERE countryCode = :code AND profileId = :profileId")
    suspend fun delete(code: String, profileId: Long)

    @Query("DELETE FROM country_notes WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
