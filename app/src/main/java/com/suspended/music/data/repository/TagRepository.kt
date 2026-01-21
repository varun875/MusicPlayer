package com.suspended.music.data.repository

import com.suspended.music.data.model.AudioTags
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor() {

    fun readTags(path: String): Result<AudioTags> {
        return try {
            val file = File(path)
            if (!file.exists()) return Result.failure(Exception("File not found"))
            
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag
            
            if (tag == null) {
                return Result.success(AudioTags())
            }
            
            val tags = AudioTags(
                title = tag.getFirst(FieldKey.TITLE) ?: "",
                artist = tag.getFirst(FieldKey.ARTIST) ?: "",
                album = tag.getFirst(FieldKey.ALBUM) ?: "",
                year = tag.getFirst(FieldKey.YEAR) ?: "",
                genre = tag.getFirst(FieldKey.GENRE) ?: ""
            )
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun writeTags(path: String, tags: AudioTags): Result<Boolean> {
        return try {
            val file = File(path)
            if (!file.exists()) return Result.failure(Exception("File not found"))
            
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tagOrCreateAndSetDefault
            
            tag.setField(FieldKey.TITLE, tags.title)
            tag.setField(FieldKey.ARTIST, tags.artist)
            tag.setField(FieldKey.ALBUM, tags.album)
            tag.setField(FieldKey.YEAR, tags.year)
            tag.setField(FieldKey.GENRE, tags.genre)
            
            AudioFileIO.write(audioFile)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
