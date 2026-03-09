package com.suspended.music.data.services

import android.content.Context
import com.suspended.music.data.api.GitHubApiService
import com.suspended.music.data.api.GitHubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCheckService {
    
    private val api: GitHubApiService

    @Inject
    constructor() {
        this.api = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }

    // For testing
    constructor(gitHubApi: GitHubApiService) {
        this.api = gitHubApi
    }

    suspend fun checkForUpdates(owner: String, repo: String): UpdateCheckResult {
        return try {
            val response = api.getLatestRelease(owner, repo)
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
        return this is Success && this.release.tagName > currentVersion
    }
    
    fun getVersionName(): String {
        return when (this) {
            is Success -> this.release.tagName
            is Error -> ""
        }
    }
}
