package com.example.hm_third_count.data.local

import androidx.room.Entity

/**
 * История просмотров деталей стран. Композитный PK (countryCode, profileId) даёт автоматическую
 * дедупликацию через INSERT OR REPLACE — повторное открытие апдейтит viewedAt, а не плодит строки.
 */
@Entity(
    tableName = "recent_views",
    primaryKeys = ["countryCode", "profileId"]
)
data class RecentViewEntity(
    val countryCode: String,
    val profileId: Long,
    val viewedAt: Long
)
