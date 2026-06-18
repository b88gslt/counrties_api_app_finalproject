package com.example.hm_third_count.data.repository

import com.example.hm_third_count.data.local.CountryNoteDao
import com.example.hm_third_count.data.local.CountryNoteEntity
import com.example.hm_third_count.data.local.VisitDao
import com.example.hm_third_count.data.local.VisitEntity
import com.example.hm_third_count.data.local.WishlistDao
import com.example.hm_third_count.data.local.WishlistEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Travel Journal — личные данные пользователя: посещённые страны, wishlist, заметки.
 * Все Flow привязаны к активному профилю через flatMapLatest — при смене профиля
 * автоматически переключаются на данные другого пользователя.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class JournalRepository @Inject constructor(
    private val visitDao: VisitDao,
    private val wishlistDao: WishlistDao,
    private val countryNoteDao: CountryNoteDao,
    private val profileRepository: ProfileRepository,
    private val clock: Clock
) {

    // ----- Visits -----

    val visits: Flow<List<VisitEntity>> = profileRepository.activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else visitDao.observeByProfile(p.id)
    }

    fun observeVisit(countryCode: String): Flow<VisitEntity?> =
        profileRepository.activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(null) else visitDao.observeOne(countryCode, p.id)
        }

    suspend fun upsertVisit(countryCode: String, visitedAt: Long, rating: Int, note: String?) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        visitDao.upsert(
            VisitEntity(
                countryCode = countryCode,
                profileId = profileId,
                visitedAt = visitedAt,
                rating = rating.coerceIn(1, 5),
                note = note?.takeIf { it.isNotBlank() },
                createdAt = clock.nowMillis()
            )
        )
    }

    suspend fun deleteVisit(countryCode: String) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        visitDao.delete(countryCode, profileId)
    }

    // ----- Wishlist -----

    val wishlist: Flow<List<WishlistEntity>> = profileRepository.activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else wishlistDao.observeByProfile(p.id)
    }

    fun observeWishlist(countryCode: String): Flow<WishlistEntity?> =
        profileRepository.activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(null) else wishlistDao.observeOne(countryCode, p.id)
        }

    suspend fun upsertWishlist(countryCode: String, priority: Int, plannedDate: Long?) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        wishlistDao.upsert(
            WishlistEntity(
                countryCode = countryCode,
                profileId = profileId,
                priority = priority.coerceIn(1, 5),
                plannedDate = plannedDate,
                addedAt = clock.nowMillis()
            )
        )
    }

    suspend fun deleteWishlist(countryCode: String) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        wishlistDao.delete(countryCode, profileId)
    }

    // ----- Notes -----

    val notes: Flow<List<CountryNoteEntity>> = profileRepository.activeProfile.flatMapLatest { p ->
        if (p == null) flowOf(emptyList()) else countryNoteDao.observeByProfile(p.id)
    }

    fun observeNote(countryCode: String): Flow<CountryNoteEntity?> =
        profileRepository.activeProfile.flatMapLatest { p ->
            if (p == null) flowOf(null) else countryNoteDao.observeOne(countryCode, p.id)
        }

    suspend fun upsertNote(countryCode: String, text: String) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        if (text.isBlank()) {
            countryNoteDao.delete(countryCode, profileId)
            return
        }
        countryNoteDao.upsert(
            CountryNoteEntity(
                countryCode = countryCode,
                profileId = profileId,
                text = text.trim(),
                updatedAt = clock.nowMillis()
            )
        )
    }

    suspend fun deleteNote(countryCode: String) {
        val profileId = profileRepository.activeProfile.first()?.id ?: return
        countryNoteDao.delete(countryCode, profileId)
    }
}
