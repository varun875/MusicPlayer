package com.suspended.music.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagRepositoryInstrumentedTest {

    @Test
    fun readTags_returnsFailure_whenFileDoesNotExist() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = TagRepository(context)
        val result = repository.readTags("C:/this/path/does/not/exist/song.mp3")

        assertTrue(result.isFailure)
        assertEquals("File not found", result.exceptionOrNull()?.message)
    }
}
