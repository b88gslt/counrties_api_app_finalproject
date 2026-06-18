package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Query("SELECT * FROM wishlist WHERE profileId = :profileId ORDER BY priority DESC, addedAt DESC")
    fun observeByProfile(profileId: Long): Flow<List<WishlistEntity>>

    @Query("SELECT * FROM wishlist WHERE countryCode = :code AND profileId = :profileId")
    fun observeOne(code: String, profileId: Long): Flow<WishlistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE countryCode = :code AND profileId = :profileId")
    suspend fun delete(code: String, profileId: Long)

    @Query("DELETE FROM wishlist WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Long)
}
