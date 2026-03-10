package com.suspended.music.data.repository

import android.test.mock.MockContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagRepositoryTest {

    @Test
    fun readTags_returnsFailure_whenFileDoesNotExist() {
        val repository = TagRepository(MockContext())
        val result = repository.readTags("C:/this/path/does/not/exist/song.mp3")

        assertTrue(result.isFailure)
        assertEquals("File not found", result.exceptionOrNull()?.message)
    }
}
