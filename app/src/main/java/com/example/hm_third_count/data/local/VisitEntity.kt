package com.example.hm_third_count.data.local

import androidx.room.Entity

/**
 * "Был в стране" — личная запись пользователя. PK по (countryCode, profileId) —
 * одна запись на пару «страна—профиль», апсёртом перезаписывается.
 */
@Entity(
    tableName = "visits",
    primaryKeys = ["countryCode", "profileId"]
)
data class VisitEntity(
    val countryCode: String,
    val profileId: Long,
    /** Дата визита как epoch millis (00:00 UTC выбранного дня). */
    val visitedAt: Long,
    /** Оценка 1..5. */
    val rating: Int,
    val note: String?,
    val createdAt: Long
)
