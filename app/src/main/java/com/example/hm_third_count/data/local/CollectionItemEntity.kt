package com.example.hm_third_count.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * M:N связь "коллекция — страна". Композитный PK + FK на collections с CASCADE —
 * удаление коллекции автоматически удаляет все её элементы.
 */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "countryCode"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("collectionId"), Index("countryCode")]
)
data class CollectionItemEntity(
    val collectionId: Long,
    val countryCode: String,
    val addedAt: Long
)
