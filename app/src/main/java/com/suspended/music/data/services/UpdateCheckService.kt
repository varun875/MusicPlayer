package com.suspended.music.data.services

import com.suspended.music.data.api.GitHubApiService
import com.suspended.music.data.api.GitHubRelease
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class UpdateCheckService @Inject constructor() {
    private val defaultApi: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubApiService::class.java)

    private var injectedApi: GitHubApiService? = null

    // Secondary constructor for testing with an injected API instance
    constructor(gitHubApi: GitHubApiService) : this() {
        injectedApi = gitHubApi
    }

    protected open suspend fun requestLatestRelease(owner: String, repo: String): Response<GitHubRelease> {
        return (injectedApi ?: defaultApi).getLatestRelease(owner, repo)
    }

    suspend fun checkForUpdates(owner: String, repo: String): UpdateCheckResult {
        return try {
            val response = requestLatestRelease(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                val release = response.body()!!
                UpdateCheckResult.Success(release)
            } else {
                UpdateCheckResult.Error("Failed to fetch release info: ${response.code()}")
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Network error: ${e.message}")
        }
    }
}

sealed class UpdateCheckResult {
    data class Success(val release: GitHubRelease) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
    
    fun isNewVersion(currentVersion: String): Boolean {
        if (this !is Success) return false
        return compareVersionNames(this.release.tagName, currentVersion) > 0
    }
    
    fun getVersionName(): String {
        return when (this) {
            is Success -> this.release.tagName
            is Error -> ""
        }
    }
}

private fun compareVersionNames(newVersion: String, currentVersion: String): Int {
    val v1 = newVersion.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
    val v2 = currentVersion.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(v1.size, v2.size)
    for (i in 0 until maxLen) {
        val n1 = v1.getOrElse(i) { 0 }
        val n2 = v2.getOrElse(i) { 0 }
        if (n1 != n2) return n1 - n2
    }
    return 0
}
