package com.example.hm_third_count.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE favorites ADD COLUMN countrySnapshotJson TEXT")
    }
}

/**
 * v3 — введение Travel Companion слоя:
 *   * profiles — локальные профили (без серверной авторизации).
 *   * country_cache — TTL-кэш стран для offline-first.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS country_cache (
                countryCode TEXT PRIMARY KEY NOT NULL,
                json TEXT NOT NULL,
                fetchedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/** v4 — recent_views: история просмотров деталей с дедупликацией по (countryCode, profileId). */
internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recent_views (
                countryCode TEXT NOT NULL,
                profileId INTEGER NOT NULL,
                viewedAt INTEGER NOT NULL,
                PRIMARY KEY(countryCode, profileId)
            )
            """.trimIndent()
        )
    }
}

/**
 * v8 — Привязываем favorites к профилю. Старая схема имела PRIMARY KEY(countryCode),
 * что делало избранное общим для всех пользователей. Переcоздаём таблицу с композитным PK
 * (countryCode, profileId) и переносим существующие записи на самый ранний профиль.
 */
internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites_new (
                countryCode TEXT NOT NULL,
                profileId INTEGER NOT NULL,
                countrySnapshotJson TEXT,
                PRIMARY KEY(countryCode, profileId)
            )
            """.trimIndent()
        )
        // Переносим существующие записи. Если профилей ещё нет (теоретически —
        // приложение ни разу не открывали), привязываем к profileId=1
        // (Default-профиль создаётся при первом запуске именно с id=1).
        db.execSQL(
            """
            INSERT OR IGNORE INTO favorites_new (countryCode, profileId, countrySnapshotJson)
            SELECT countryCode,
                   COALESCE((SELECT MIN(id) FROM profiles), 1),
                   countrySnapshotJson
            FROM favorites
            """.trimIndent()
        )
        db.execSQL("DROP TABLE favorites")
        db.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
    }
}

/** v7 — saved_filters: пресеты фильтра (имя + region + showFavoritesOnly) для активного профиля. */
internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_filters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                profileId INTEGER NOT NULL,
                name TEXT NOT NULL,
                region TEXT NOT NULL,
                showFavoritesOnly INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/** v6 — Collections: пользовательские списки стран с M:N-связью + CASCADE при удалении коллекции. */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS collections (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                profileId INTEGER NOT NULL,
                name TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                position INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS collection_items (
                collectionId INTEGER NOT NULL,
                countryCode TEXT NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY(collectionId, countryCode),
                FOREIGN KEY(collectionId) REFERENCES collections(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_items_collectionId ON collection_items(collectionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_items_countryCode ON collection_items(countryCode)")
    }
}

/** v5 — Travel Journal: visits + wishlist + country_notes (одна запись на пару страна-профиль). */
internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS visits (
                countryCode TEXT NOT NULL,
                profileId INTEGER NOT NULL,
                visitedAt INTEGER NOT NULL,
                rating INTEGER NOT NULL,
                note TEXT,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(countryCode, profileId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wishlist (
                countryCode TEXT NOT NULL,
                profileId INTEGER NOT NULL,
                priority INTEGER NOT NULL,
                plannedDate INTEGER,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY(countryCode, profileId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS country_notes (
                countryCode TEXT NOT NULL,
                profileId INTEGER NOT NULL,
                text TEXT NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(countryCode, profileId)
            )
            """.trimIndent()
        )
    }
}
