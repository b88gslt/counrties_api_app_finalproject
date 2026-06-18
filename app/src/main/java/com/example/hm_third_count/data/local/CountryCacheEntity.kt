package com.example.hm_third_count.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TTL-кэш стран для offline-first.
 * Хранит JSON-снимок страны и timestamp последнего обновления.
 */
@Entity(tableName = "country_cache")
data class CountryCacheEntity(
    @PrimaryKey val countryCode: String,
    val json: String,
    val fetchedAt: Long
)
