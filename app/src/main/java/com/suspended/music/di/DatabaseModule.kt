package com.suspended.music.di

import android.content.Context
import androidx.room.Room
import com.suspended.music.data.local.AlbumDao
import com.suspended.music.data.local.ArtistDao
import com.suspended.music.data.local.MIGRATION_1_2
import com.suspended.music.data.local.MusicDatabase
import com.suspended.music.data.local.PlaylistDao
import com.suspended.music.data.local.SongDao
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
    fun provideMusicDatabase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            MusicDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSongDao(database: MusicDatabase): SongDao = database.songDao()
    
    @Provides
    @Singleton
    fun provideAlbumDao(database: MusicDatabase): AlbumDao = database.albumDao()
    
    @Provides
    @Singleton
    fun provideArtistDao(database: MusicDatabase): ArtistDao = database.artistDao()
    
    @Provides
    @Singleton
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao = database.playlistDao()
}
