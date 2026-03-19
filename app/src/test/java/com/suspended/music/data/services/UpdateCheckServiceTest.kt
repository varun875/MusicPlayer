package com.suspended.music.data.services

import com.suspended.music.data.api.GitHubApiService
import com.suspended.music.data.api.GitHubRelease
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.UnknownHostException

class UpdateCheckServiceTest {

    private fun fakeRelease(tag: String) = GitHubRelease(
        tagName = tag,
        name = "release",
        draft = false,
        prerelease = false,
        publishedAt = "2026-01-01T00:00:00Z",
        body = "",
        htmlUrl = "https://example.com"
    )

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
    fun testNetworkError_viaInjectedApi() = runBlocking {
        val mockApi = object : GitHubApiService {
            override suspend fun getLatestRelease(owner: String, repo: String): Response<GitHubRelease> {
                throw UnknownHostException("Unable to resolve host \"api.github.com\"")
            }
        }

        val service = UpdateCheckService(mockApi)
        val result = service.checkForUpdates("owner", "repo")

        assertTrue(result is UpdateCheckResult.Error)
        assertEquals(
            "Network error: Unable to resolve host \"api.github.com\"",
            (result as UpdateCheckResult.Error).message
        )
    }

    @Test
    fun testFailedToFetchReleaseInfo() = runBlocking {
        val mockApi = object : GitHubApiService {
            override suspend fun getLatestRelease(owner: String, repo: String): Response<GitHubRelease> {
                return Response.error(404, okhttp3.ResponseBody.Companion.create(null, ""))
            }
        }

        val service = UpdateCheckService(mockApi)
        val result = service.checkForUpdates("owner", "repo")

        assertTrue(result is UpdateCheckResult.Error)
        assertEquals("Failed to fetch release info: 404", (result as UpdateCheckResult.Error).message)
    }

    @Test
    fun testSuccess() = runBlocking {
        val mockApi = object : GitHubApiService {
            override suspend fun getLatestRelease(owner: String, repo: String): Response<GitHubRelease> {
                val release = GitHubRelease(
                    tagName = "v1.0.0",
                    name = "Release 1.0.0",
                    draft = false,
                    prerelease = false,
                    publishedAt = "2023-01-01T00:00:00Z",
                    body = "Release notes",
                    htmlUrl = "https://github.com/owner/repo/releases/tag/v1.0.0"
                )
                return Response.success(release)
            }
        }

        val service = UpdateCheckService(mockApi)
        val result = service.checkForUpdates("owner", "repo")

        assertTrue(result is UpdateCheckResult.Success)
        val successResult = result as UpdateCheckResult.Success
        assertEquals("v1.0.0", successResult.release.tagName)
        assertEquals("Release 1.0.0", successResult.release.name)
        assertEquals("https://github.com/owner/repo/releases/tag/v1.0.0", successResult.release.htmlUrl)
    }

    @Test
    fun isNewVersion_handlesSemanticVersionsCorrectly() {
        val newer = UpdateCheckResult.Success(release = fakeRelease("v1.10.0"))
        val older = UpdateCheckResult.Success(release = fakeRelease("v1.2.0"))
        val error = UpdateCheckResult.Error("network")

        assertTrue(newer.isNewVersion("1.9.9"))
        assertFalse(older.isNewVersion("1.10.0"))
        assertFalse(error.isNewVersion("0.0.1"))
    }
}
