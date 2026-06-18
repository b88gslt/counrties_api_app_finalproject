package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.CollectionDao
import com.example.hm_third_count.data.local.CollectionEntity
import com.example.hm_third_count.data.local.CollectionItemDao
import com.example.hm_third_count.data.local.CollectionItemEntity
import com.example.hm_third_count.data.local.CollectionItemTuple
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Коллекции = пользовательские списки стран. Связь M:N через collection_items.
 * Все Flow привязаны к активному профилю через flatMapLatest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CollectionsRepository @Inject constructor(
    private val collectionDao: CollectionDao,
    private val collectionItemDao: CollectionItemDao,
    private val profileRepository: ProfileRepository,
    private val clock: Clock
) {

    /** Все коллекции активного профиля. */
    val collections: Flow<List<CollectionEntity>> = profileRepository.activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else collectionDao.observeByProfile(p.id)
    }

    /** Связи (collectionId, countryCode) для активного профиля — для combine на экранах. */
    val allItems: Flow<List<CollectionItemTuple>> = profileRepository.activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else collectionItemDao.observeAllForProfile(p.id)
    }

    fun observeCollection(id: Long): Flow<CollectionEntity?> = collectionDao.observeOne(id)

    fun observeCountryCodesInCollection(id: Long): Flow<List<String>> =
        collectionItemDao.observeByCollection(id).map { items -> items.map { it.countryCode } }

    /** В каких коллекциях активного профиля находится эта страна. */
    fun observeCollectionsForCountry(code: String): Flow<List<Long>> =
        profileRepository.activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else collectionItemDao.observeCollectionsForCountry(code, p.id)
        }

    suspend fun create(name: String, colorHex: String): Long {
        val profile = profileRepository.activeProfile.first() ?: return -1L
        val nextPos = collectionDao.maxPosition(profile.id) + 1
        return collectionDao.insert(
            CollectionEntity(
                profileId = profile.id,
                name = name.trim(),
                colorHex = colorHex,
                position = nextPos,
                createdAt = clock.nowMillis()
            )
        )
    }

    suspend fun rename(id: Long, name: String) {
        val current = collectionDao.getById(id) ?: return
        collectionDao.update(current.copy(name = name.trim()))
    }

    suspend fun changeColor(id: Long, colorHex: String) {
        val current = collectionDao.getById(id) ?: return
        collectionDao.update(current.copy(colorHex = colorHex))
    }

    /** Удаление коллекции — connection_items уходят каскадом через FK. */
    suspend fun delete(id: Long) {
        collectionDao.deleteById(id)
    }

    suspend fun addCountry(collectionId: Long, countryCode: String) {
        collectionItemDao.upsert(
            CollectionItemEntity(
                collectionId = collectionId,
                countryCode = countryCode,
                addedAt = clock.nowMillis()
            )
        )
    }

    suspend fun removeCountry(collectionId: Long, countryCode: String) {
        collectionItemDao.delete(collectionId, countryCode)
    }

    /**
     * Перезаписать членство страны в коллекциях ровно набором selectedIds —
     * добавить недостающие, убрать лишние.
     */
    suspend fun setCountryMembership(countryCode: String, selectedIds: Set<Long>) {
        val profile = profileRepository.activeProfile.first() ?: return
        val current = collectionItemDao
            .observeCollectionsForCountry(countryCode, profile.id)
            .first()
            .toSet()
        val toAdd = selectedIds - current
        val toRemove = current - selectedIds
        toAdd.forEach { id -> addCountry(id, countryCode) }
        toRemove.forEach { id -> removeCountry(id, countryCode) }
    }
}
