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
