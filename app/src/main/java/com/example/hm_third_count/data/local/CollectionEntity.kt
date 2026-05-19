package com.example.hm_third_count.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val colorHex: String,
    /** Порядок отображения — для будущего drag-and-drop. */
    val position: Int,
    val createdAt: Long
)
