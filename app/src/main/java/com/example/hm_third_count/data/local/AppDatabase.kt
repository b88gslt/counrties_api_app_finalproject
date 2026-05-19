package com.example.hm_third_count.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        ProfileEntity::class,
        CountryCacheEntity::class,
        RecentViewEntity::class,
        VisitEntity::class,
        WishlistEntity::class,
        CountryNoteEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        SavedFilterEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun profileDao(): ProfileDao
    abstract fun countryCacheDao(): CountryCacheDao
    abstract fun recentViewDao(): RecentViewDao
    abstract fun visitDao(): VisitDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun countryNoteDao(): CountryNoteDao
    abstract fun collectionDao(): CollectionDao
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun savedFilterDao(): SavedFilterDao
}
