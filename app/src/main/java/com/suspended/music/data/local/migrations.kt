package com.suspended.music.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for version updates
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create indexes for better query performance
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_albumId` ON `songs` (`albumId`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_isFavorite` ON `songs` (`isFavorite`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_dateAdded` ON `songs` (`dateAdded`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_lastPlayed` ON `songs` (`lastPlayed`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_albums_artist` ON `albums` (`artist`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_albums_dateAdded` ON `albums` (`dateAdded`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_artists_name` ON `artists` (`name`)
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add additional indexes for improved search and sorting performance
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_genre` ON `songs` (`genre`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_playCount` ON `songs` (`playCount`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_duration` ON `songs` (`duration`)
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create the folders table added in version 4
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `folders` (
                `path` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `songCount` INTEGER NOT NULL DEFAULT 0,
                `parentPath` TEXT,
                PRIMARY KEY(`path`)
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `songs_fts`")
        database.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS `songs_fts` 
            USING fts4(title TEXT, artist TEXT, album TEXT, content=`songs`)
        """.trimIndent())
        database.execSQL("INSERT INTO `songs_fts` (`songs_fts`) VALUES ('rebuild')")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // New `composer` column for expanded search filters (composer scope).
        database.execSQL("""
            ALTER TABLE `songs` ADD COLUMN `composer` TEXT NOT NULL DEFAULT ''
        """.trimIndent())
        // Rebuild the FTS table with the composer column so full-text search
        // covers composer metadata too. lyrics stays in the content table and is
        // matched via LIKE (its text is long and only used as an opt-in filter).
        database.execSQL("DROP TABLE IF EXISTS `songs_fts`")
        database.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS `songs_fts` 
            USING fts4(title TEXT, artist TEXT, album TEXT, composer TEXT, content=`songs`)
        """.trimIndent())
        database.execSQL("INSERT INTO `songs_fts` (`songs_fts`) VALUES ('rebuild')")
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_dateModified` ON `songs` (`dateModified`)
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Folder filtering queries songs.path; without an index those scan the table.
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_path` ON `songs` (`path`)
        """.trimIndent())

        // Recreate `folders` canonically: MIGRATION_3_4 created `songCount` with
        // DEFAULT 0 while the entity declared no default, which Room's schema
        // validation can reject on open. The table holds no data, so this is lossless.
        database.execSQL("DROP TABLE IF EXISTS `folders`")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `folders` (
                `path` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `songCount` INTEGER NOT NULL DEFAULT 0,
                `parentPath` TEXT,
                PRIMARY KEY(`path`)
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Sort-by-size paging orders on `size`; without an index that is a full sort.
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_size` ON `songs` (`size`)
        """.trimIndent())
    }
}
