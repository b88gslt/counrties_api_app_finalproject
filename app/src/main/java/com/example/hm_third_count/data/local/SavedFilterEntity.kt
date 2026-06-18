package com.example.hm_third_count.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сохранённый пресет фильтра списка стран — имя + комбинация фильтров,
 * которую пользователь применил и хочет сохранить «на потом».
 */
@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val region: String,
    val showFavoritesOnly: Boolean,
    val createdAt: Long
)
