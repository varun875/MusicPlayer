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
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val gitHubApi = retrofit.create(GitHubApiService::class.java)

    protected open suspend fun requestLatestRelease(owner: String, repo: String): Response<GitHubRelease> {
        return gitHubApi.getLatestRelease(owner, repo)
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
    val left = normalizeVersion(newVersion)
    val right = normalizeVersion(currentVersion)
    val maxSize = maxOf(left.size, right.size)

    for (index in 0 until maxSize) {
        val l = left.getOrElse(index) { 0 }
        val r = right.getOrElse(index) { 0 }
        if (l != r) return l.compareTo(r)
    }
    return 0
}

private fun normalizeVersion(version: String): List<Int> {
    val cleaned = version.trim().removePrefix("v").removePrefix("V")
    if (cleaned.isEmpty()) return emptyList()

    return cleaned.split(".", "-", "+")
        .filter { it.isNotBlank() }
        .map { token ->
            val numericPrefix = token.takeWhile { it.isDigit() }
            numericPrefix.toIntOrNull() ?: 0
        }
}
