package com.example.hm_third_count.testutil

import com.example.hm_third_count.data.local.CollectionEntity
import com.example.hm_third_count.data.local.CollectionItemDao
import com.example.hm_third_count.data.local.CollectionItemEntity
import com.example.hm_third_count.data.local.CollectionItemTuple
import com.example.hm_third_count.data.local.CountryNoteDao
import com.example.hm_third_count.data.local.CountryNoteEntity
import com.example.hm_third_count.data.local.RecentViewDao
import com.example.hm_third_count.data.local.RecentViewEntity
import com.example.hm_third_count.data.local.VisitDao
import com.example.hm_third_count.data.local.VisitEntity
import com.example.hm_third_count.data.local.WishlistDao
import com.example.hm_third_count.data.local.WishlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory фейки DAO с поведением максимально близким к настоящему Room:
 * композитные PK через ключи на уровне Map. Используются вместо MockK
 * там, где важно проверять реальную семантику UPSERT/DELETE.
 */

class FakeRecentViewDao : RecentViewDao {
    private val data = MutableStateFlow<Map<Pair<String, Long>, RecentViewEntity>>(emptyMap())

    override fun observeByProfile(profileId: Long): Flow<List<RecentViewEntity>> =
        data.map { map ->
            map.values.filter { it.profileId == profileId }.sortedByDescending { it.viewedAt }
        }

    override suspend fun upsert(entity: RecentViewEntity) {
        val key = entity.countryCode to entity.profileId
        data.value = data.value + (key to entity)
    }

    override suspend fun delete(countryCode: String, profileId: Long) {
        data.value = data.value - (countryCode to profileId)
    }

    override suspend fun clearForProfile(profileId: Long) {
        data.value = data.value.filterKeys { it.second != profileId }
    }

    override suspend fun countForProfile(profileId: Long): Int =
        data.value.values.count { it.profileId == profileId }

    fun snapshot(): List<RecentViewEntity> = data.value.values.toList()
}

class FakeVisitDao : VisitDao {
    private val data = MutableStateFlow<Map<Pair<String, Long>, VisitEntity>>(emptyMap())

    override fun observeByProfile(profileId: Long): Flow<List<VisitEntity>> =
        data.map { map ->
            map.values.filter { it.profileId == profileId }.sortedByDescending { it.visitedAt }
        }

    override fun observeOne(code: String, profileId: Long): Flow<VisitEntity?> =
        data.map { it[code to profileId] }

    override suspend fun upsert(entity: VisitEntity) {
        val key = entity.countryCode to entity.profileId
        data.value = data.value + (key to entity)
    }

    override suspend fun delete(code: String, profileId: Long) {
        data.value = data.value - (code to profileId)
    }

    override suspend fun deleteByProfile(profileId: Long) {
        data.value = data.value.filterKeys { it.second != profileId }
    }

    fun snapshot(): List<VisitEntity> = data.value.values.toList()
}

class FakeWishlistDao : WishlistDao {
    private val data = MutableStateFlow<Map<Pair<String, Long>, WishlistEntity>>(emptyMap())

    override fun observeByProfile(profileId: Long): Flow<List<WishlistEntity>> =
        data.map { map -> map.values.filter { it.profileId == profileId } }

    override fun observeOne(code: String, profileId: Long): Flow<WishlistEntity?> =
        data.map { it[code to profileId] }

    override suspend fun upsert(entity: WishlistEntity) {
        val key = entity.countryCode to entity.profileId
        data.value = data.value + (key to entity)
    }

    override suspend fun delete(code: String, profileId: Long) {
        data.value = data.value - (code to profileId)
    }

    override suspend fun deleteByProfile(profileId: Long) {
        data.value = data.value.filterKeys { it.second != profileId }
    }
}

class FakeCountryNoteDao : CountryNoteDao {
    private val data = MutableStateFlow<Map<Pair<String, Long>, CountryNoteEntity>>(emptyMap())

    override fun observeByProfile(profileId: Long): Flow<List<CountryNoteEntity>> =
        data.map { map -> map.values.filter { it.profileId == profileId } }

    override fun observeOne(code: String, profileId: Long): Flow<CountryNoteEntity?> =
        data.map { it[code to profileId] }

    override suspend fun upsert(entity: CountryNoteEntity) {
        val key = entity.countryCode to entity.profileId
        data.value = data.value + (key to entity)
    }

    override suspend fun delete(code: String, profileId: Long) {
        data.value = data.value - (code to profileId)
    }

    override suspend fun deleteByProfile(profileId: Long) {
        data.value = data.value.filterKeys { it.second != profileId }
    }
}

class FakeCollectionItemDao : CollectionItemDao {
    private val data = MutableStateFlow<Map<Pair<Long, String>, CollectionItemEntity>>(emptyMap())
    private val collectionProfiles = mutableMapOf<Long, Long>()

    fun assignCollectionToProfile(collectionId: Long, profileId: Long) {
        collectionProfiles[collectionId] = profileId
    }

    override fun observeByCollection(collectionId: Long): Flow<List<CollectionItemEntity>> =
        data.map { map -> map.values.filter { it.collectionId == collectionId } }

    override fun observeCollectionsForCountry(code: String, profileId: Long): Flow<List<Long>> =
        data.map { map ->
            map.values
                .filter { it.countryCode == code && collectionProfiles[it.collectionId] == profileId }
                .map { it.collectionId }
        }

    override fun observeAllForProfile(profileId: Long): Flow<List<CollectionItemTuple>> =
        data.map { map ->
            map.values
                .filter { collectionProfiles[it.collectionId] == profileId }
                .map { CollectionItemTuple(it.collectionId, it.countryCode) }
        }

    override suspend fun upsert(entity: CollectionItemEntity) {
        val key = entity.collectionId to entity.countryCode
        data.value = data.value + (key to entity)
    }

    override suspend fun delete(collectionId: Long, code: String) {
        data.value = data.value - (collectionId to code)
    }

    fun snapshot(): List<CollectionItemEntity> = data.value.values.toList()
}
