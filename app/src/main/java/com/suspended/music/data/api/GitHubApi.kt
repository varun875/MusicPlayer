package com.suspended.music.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.Response

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
    val htmlUrl: String
)

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @retrofit2.http.Path("owner") owner: String,
        @retrofit2.http.Path("repo") repo: String
    ): Response<GitHubRelease>
}
