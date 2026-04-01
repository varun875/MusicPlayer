package com.suspended.music.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Represents a single released version on GitHub.
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("draft")
    val draft: Boolean,
    
    @SerializedName("prerelease")
    val prerelease: Boolean,
    
    @SerializedName("published_at")
    val publishedAt: String,
    
    @SerializedName("body")
    val body: String,
    
    @SerializedName("html_url")
    val htmlUrl: String,
    
    @SerializedName("assets")
    val assets: List<GitHubAsset>? = emptyList()
) {
    /** Convenient helper to instantly locate the APK download link from the release attachments */
    val apkAsset: GitHubAsset?
        get() = assets?.find { it.name.endsWith(".apk", ignoreCase = true) }
}

/**
 * Represents a compiled binary file (e.g., APK) attached to a GitHub release.
 */
data class GitHubAsset(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("size")
    val sizeBytes: Long,
    
    @SerializedName("download_count")
    val downloadCount: Int,
    
    @SerializedName("browser_download_url")
    val downloadUrl: String
)

interface GitHubApiService {
    
    /**
     * Fetches the latest stable release from the specified GitHub repository.
     * Enforces required GitHub API V3 accept headers for stability.
     */
    @Headers("Accept: application/vnd.github.v3+json")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>
    
    /**
     * Fetches all recent releases (including pre-releases/betas).
     * Useful if the user opts into beta updates instead of strict stability.
     */
    @Headers("Accept: application/vnd.github.v3+json")
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getAllReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 5
    ): Response<List<GitHubRelease>>
}
