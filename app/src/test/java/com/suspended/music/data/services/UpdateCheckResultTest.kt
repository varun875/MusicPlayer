package com.suspended.music.data.services

import com.suspended.music.data.api.GitHubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckResultTest {

    private fun createRelease(tagName: String): GitHubRelease {
        return GitHubRelease(
            tagName = tagName,
            name = "Test Release",
            draft = false,
            prerelease = false,
            publishedAt = "2024-01-01T00:00:00Z",
            body = "Release notes",
            htmlUrl = "https://github.com/example/repo/releases/tag/$tagName"
        )
    }

    @Test
    fun isNewVersion_successAndNewerVersion_returnsTrue() {
        val result = UpdateCheckResult.Success(createRelease("v1.2.0"))
        assertTrue(result.isNewVersion("v1.1.0"))
    }

    @Test
    fun isNewVersion_successAndSameVersion_returnsFalse() {
        val result = UpdateCheckResult.Success(createRelease("v1.0.0"))
        assertFalse(result.isNewVersion("v1.0.0"))
    }

    @Test
    fun isNewVersion_successAndOlderVersion_returnsFalse() {
        val result = UpdateCheckResult.Success(createRelease("v1.0.0"))
        assertFalse(result.isNewVersion("v1.1.0"))
    }

    @Test
    fun isNewVersion_error_returnsFalse() {
        val result = UpdateCheckResult.Error("Network error")
        assertFalse(result.isNewVersion("v1.0.0"))
    }

    @Test
    fun getVersionName_success_returnsTagName() {
        val result = UpdateCheckResult.Success(createRelease("v2.0.0"))
        assertEquals("v2.0.0", result.getVersionName())
    }

    @Test
    fun getVersionName_error_returnsEmptyString() {
        val result = UpdateCheckResult.Error("Server error")
        assertEquals("", result.getVersionName())
    }
}
