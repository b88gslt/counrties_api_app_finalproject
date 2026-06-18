package com.example.hm_third_count.data.local

import androidx.room.Entity

/**
 * Избранное — теперь привязано к профилю. Композитный PK (countryCode, profileId)
 * позволяет одной стране быть в избранном у разных пользователей независимо.
 */
@Entity(
    tableName = "favorites",
    primaryKeys = ["countryCode", "profileId"]
)
data class FavoriteEntity(
    val countryCode: String,
    val profileId: Long,
    /** JSON-снимок для офлайна — оставляем для совместимости со старой схемой. */
    val countrySnapshotJson: String? = null
)
