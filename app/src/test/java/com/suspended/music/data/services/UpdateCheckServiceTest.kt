package com.suspended.music.data.services

import com.suspended.music.data.api.GitHubRelease
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class UpdateCheckServiceTest {

    @Test
    fun checkForUpdates_returnsError_whenNetworkThrows() = runBlocking {
        val service = object : UpdateCheckService() {
            override suspend fun requestLatestRelease(owner: String, repo: String): Response<GitHubRelease> {
                throw IOException("timeout")
            }
        }

        val result = service.checkForUpdates("owner", "repo")
        assertTrue(result is UpdateCheckResult.Error)
        assertTrue((result as UpdateCheckResult.Error).message.contains("Network error"))
        assertTrue(result.message.contains("timeout"))
    }

    @Test
    fun isNewVersion_handlesSemanticVersionsCorrectly() {
        val newer = UpdateCheckResult.Success(
            release = fakeRelease("v1.10.0")
        )
        val older = UpdateCheckResult.Success(
            release = fakeRelease("v1.2.0")
        )
        val error = UpdateCheckResult.Error("network")

        assertTrue(newer.isNewVersion("1.9.9"))
        assertFalse(older.isNewVersion("1.10.0"))
        assertFalse(error.isNewVersion("0.0.1"))
    }

    private fun fakeRelease(tag: String) = GitHubRelease(
        tagName = tag,
        name = "release",
        draft = false,
        prerelease = false,
        publishedAt = "2026-01-01T00:00:00Z",
        body = "",
        htmlUrl = "https://example.com"
    )
}
