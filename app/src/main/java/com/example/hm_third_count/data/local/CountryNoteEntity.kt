package com.example.hm_third_count.data.local

import androidx.room.Entity

@Entity(
    tableName = "country_notes",
    primaryKeys = ["countryCode", "profileId"]
)
data class CountryNoteEntity(
    val countryCode: String,
    val profileId: Long,
    val text: String,
    val updatedAt: Long
)
