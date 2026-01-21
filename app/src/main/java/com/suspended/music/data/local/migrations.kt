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
