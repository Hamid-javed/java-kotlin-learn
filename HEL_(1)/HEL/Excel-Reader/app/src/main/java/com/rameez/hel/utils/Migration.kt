package com.rameez.hel.utils

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN modifiedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN displayCountUpdatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN readCountUpdatedAt INTEGER NOT NULL DEFAULT 0")
        // Optionally set createdAt/modifiedAt to current time for legacy rows:
        database.execSQL("UPDATE WIP_LIST SET createdAt = (strftime('%s','now') * 1000) WHERE createdAt = 0")
        database.execSQL("UPDATE WIP_LIST SET modifiedAt = (strftime('%s','now') * 1000) WHERE modifiedAt = 0")
    }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns with default 0
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN firstViewedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN firstEncounteredAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `GENERATED_ARTICLES` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE GENERATED_ARTICLES ADD COLUMN wipIds TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE WIP_LIST ADD COLUMN lastParaCreatedAt INTEGER NOT NULL DEFAULT 0")
    }
}
