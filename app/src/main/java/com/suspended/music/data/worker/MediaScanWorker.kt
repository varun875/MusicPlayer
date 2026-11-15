package com.suspended.music.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suspended.music.data.repository.MediaScanner
import com.suspended.music.data.repository.ScanResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class MediaScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaScanner: MediaScanner,
    private val scanStatusStore: ScanStatusStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting background media scan")
            val result = mediaScanner.scanMedia()
            scanStatusStore.publish(result)
            when (result) {
                is ScanResult.Success -> {
                    Log.i(TAG, "Background scan completed: ${result.songCount} songs, ${result.albumCount} albums, ${result.artistCount} artists")
                    Result.success()
                }
                is ScanResult.AlreadyInProgress -> {
                    // State conflict, not a transient fault — retrying would
                    // just collide with the running scan again.
                    Log.w(TAG, "Background scan deferred: ${result.message}")
                    Result.success()
                }
                is ScanResult.Error -> {
                    Log.e(TAG, "Background scan failed: ${result.message}")
                    if (runAttemptCount + 1 < MAX_RETRY_ATTEMPTS) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: CancellationException) {
            // Cooperative cancellation must propagate, not become a retry.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Background scan exception: ${e.message}", e)
            scanStatusStore.publish(ScanResult.Error(e.message ?: "Media scan failed"))
            if (runAttemptCount + 1 < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "MediaScanWorker"
        const val WORK_NAME = "media_scan_work"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}