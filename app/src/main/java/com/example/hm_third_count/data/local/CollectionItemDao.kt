package com.example.hm_third_count.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionItemDao {

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun observeByCollection(collectionId: Long): Flow<List<CollectionItemEntity>>

    /** Коллекции активного профиля, содержащие эту страну (для деталки). */
    @Query(
        """
        SELECT ci.collectionId FROM collection_items ci
        INNER JOIN collections c ON c.id = ci.collectionId
        WHERE ci.countryCode = :code AND c.profileId = :profileId
        """
    )
    fun observeCollectionsForCountry(code: String, profileId: Long): Flow<List<Long>>

    /**
     * Все элементы коллекций активного профиля одним JOIN'ом — удобно для combine на главной.
     * Возвращает пары (collectionId, countryCode).
     */
    @Query(
        """
        SELECT ci.collectionId AS collectionId, ci.countryCode AS countryCode
        FROM collection_items ci
        INNER JOIN collections c ON c.id = ci.collectionId
        WHERE c.profileId = :profileId
        """
    )
    fun observeAllForProfile(profileId: Long): Flow<List<CollectionItemTuple>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND countryCode = :code")
    suspend fun delete(collectionId: Long, code: String)
}

data class CollectionItemTuple(
    val collectionId: Long,
    val countryCode: String
)
