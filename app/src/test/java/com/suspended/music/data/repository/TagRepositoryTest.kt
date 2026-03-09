package com.suspended.music.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class TagRepositoryTest {

    private lateinit var tagRepository: TagRepository
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk()
        // Stub the methods that might be called in constructor or setup
        every { mockContext.cacheDir } returns File(System.getProperty("java.io.tmpdir"))

        tagRepository = TagRepository(mockContext)
    }

    @Test
    fun readTags_fileNotFound_returnsFailure() {
        // Arrange
        val nonExistentPath = "this/path/does/not/exist.mp3"

        // Act
        val result = tagRepository.readTags(nonExistentPath)

        // Assert
        assertTrue("Result should be a failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Exception should not be null", exception != null)
        assertEquals("File not found", exception?.message)
    }
}
