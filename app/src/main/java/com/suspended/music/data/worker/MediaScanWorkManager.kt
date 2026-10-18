package com.suspended.music.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleImmediateScan() {
        // No constraints — this is a user-initiated action and should always run.
        val scanRequest = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            MediaScanWorker.WORK_NAME,
            // KEEP so a new request doesn't cancel a scan that is already running
            // (which would then surface as a spurious "already in progress" retry).
            ExistingWorkPolicy.KEEP,
            scanRequest
        )
    }
}