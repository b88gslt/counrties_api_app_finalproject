package com.example.hm_third_count.data.local

import androidx.room.Entity

@Entity(
    tableName = "wishlist",
    primaryKeys = ["countryCode", "profileId"]
)
data class WishlistEntity(
    val countryCode: String,
    val profileId: Long,
    /** Приоритет 1..5 (5 — хочу больше всего). */
    val priority: Int,
    val plannedDate: Long?,
    val addedAt: Long
)
