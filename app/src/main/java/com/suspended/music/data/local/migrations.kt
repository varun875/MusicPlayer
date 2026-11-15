package com.suspended.music.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Database migrations for version updates
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // Create indexes for better query performance
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_albumId` ON `songs` (`albumId`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_isFavorite` ON `songs` (`isFavorite`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_dateAdded` ON `songs` (`dateAdded`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_lastPlayed` ON `songs` (`lastPlayed`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_albums_artist` ON `albums` (`artist`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_albums_dateAdded` ON `albums` (`dateAdded`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_artists_name` ON `artists` (`name`)
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        // Add additional indexes for improved search and sorting performance
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_genre` ON `songs` (`genre`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_playCount` ON `songs` (`playCount`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_duration` ON `songs` (`duration`)
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        // Create the folders table added in version 4
        connection.execSQL("""
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
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `songs_fts`")
        connection.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS `songs_fts` 
            USING fts4(title TEXT, artist TEXT, album TEXT, content=`songs`)
        """.trimIndent())
        connection.execSQL("INSERT INTO `songs_fts` (`songs_fts`) VALUES ('rebuild')")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // New `composer` column for expanded search filters (composer scope).
        connection.execSQL("""
            ALTER TABLE `songs` ADD COLUMN `composer` TEXT NOT NULL DEFAULT ''
        """.trimIndent())
        // Rebuild the FTS table with the composer column so full-text search
        // covers composer metadata too. lyrics stays in the content table and is
        // matched via LIKE (its text is long and only used as an opt-in filter).
        //
        // Room's content-sync triggers live on `songs` and are NOT dropped when
        // the FTS table is dropped, so they still reference the old 3-column
        // layout — recreate them for the 4-column table (same names Room uses,
        // see the generated MusicDatabase_Impl).
        connection.execSQL("DROP TABLE IF EXISTS `songs_fts`")
        connection.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS `songs_fts` 
            USING fts4(title TEXT, artist TEXT, album TEXT, composer TEXT, content=`songs`)
        """.trimIndent())
        connection.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_songs_fts_AFTER_INSERT")
        connection.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_songs_fts_AFTER_UPDATE")
        connection.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_songs_fts_AFTER_DELETE")
        connection.execSQL("""
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_songs_fts_AFTER_INSERT AFTER INSERT ON `songs` BEGIN INSERT INTO `songs_fts`(`docid`, `title`, `artist`, `album`, `composer`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`artist`, NEW.`album`, NEW.`composer`); END
        """.trimIndent())
        connection.execSQL("""
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_songs_fts_AFTER_UPDATE AFTER UPDATE ON `songs` BEGIN DELETE FROM `songs_fts` WHERE `docid` = OLD.`rowid`; INSERT INTO `songs_fts`(`docid`, `title`, `artist`, `album`, `composer`) VALUES (NEW.`rowid`, NEW.`title`, NEW.`artist`, NEW.`album`, NEW.`composer`); END
        """.trimIndent())
        connection.execSQL("""
            CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_songs_fts_AFTER_DELETE AFTER DELETE ON `songs` BEGIN DELETE FROM `songs_fts` WHERE `docid` = OLD.`rowid`; END
        """.trimIndent())
        connection.execSQL("INSERT INTO `songs_fts` (`songs_fts`) VALUES ('rebuild')")
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_dateModified` ON `songs` (`dateModified`)
        """.trimIndent())
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        // Folder filtering queries songs.path; without an index those scan the table.
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_path` ON `songs` (`path`)
        """.trimIndent())
        // Note: `folders` already matches the entity (DEFAULT 0 on both sides), so
        // recreating it here would be a destructive no-op that silently wipes any
        // data in it. The table is unused (no DAO exposes it); left in place.
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Sort-by-size paging orders on `size`; without an index that is a full sort.
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_songs_size` ON `songs` (`size`)
        """.trimIndent())
    }
}
