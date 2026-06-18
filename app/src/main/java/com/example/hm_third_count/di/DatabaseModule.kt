package com.example.hm_third_count.di

import android.content.Context
import androidx.room.Room
import com.example.hm_third_count.data.local.AppDatabase
import com.example.hm_third_count.data.local.CountryCacheDao
import com.example.hm_third_count.data.local.FavoriteDao
import com.example.hm_third_count.data.local.CollectionDao
import com.example.hm_third_count.data.local.CollectionItemDao
import com.example.hm_third_count.data.local.CountryNoteDao
import com.example.hm_third_count.data.local.MIGRATION_1_2
import com.example.hm_third_count.data.local.MIGRATION_2_3
import com.example.hm_third_count.data.local.MIGRATION_3_4
import com.example.hm_third_count.data.local.MIGRATION_4_5
import com.example.hm_third_count.data.local.MIGRATION_5_6
import com.example.hm_third_count.data.local.MIGRATION_6_7
import com.example.hm_third_count.data.local.MIGRATION_7_8
import com.example.hm_third_count.data.local.ProfileDao
import com.example.hm_third_count.data.local.RecentViewDao
import com.example.hm_third_count.data.local.SavedFilterDao
import com.example.hm_third_count.data.local.VisitDao
import com.example.hm_third_count.data.local.WishlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "countries_db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8
            )
            .build()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideCountryCacheDao(db: AppDatabase): CountryCacheDao = db.countryCacheDao()

    @Provides
    fun provideRecentViewDao(db: AppDatabase): RecentViewDao = db.recentViewDao()

    @Provides
    fun provideVisitDao(db: AppDatabase): VisitDao = db.visitDao()

    @Provides
    fun provideWishlistDao(db: AppDatabase): WishlistDao = db.wishlistDao()

    @Provides
    fun provideCountryNoteDao(db: AppDatabase): CountryNoteDao = db.countryNoteDao()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideCollectionItemDao(db: AppDatabase): CollectionItemDao = db.collectionItemDao()

    @Provides
    fun provideSavedFilterDao(db: AppDatabase): SavedFilterDao = db.savedFilterDao()
}
