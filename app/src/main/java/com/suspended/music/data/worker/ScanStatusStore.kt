package com.suspended.music.data.worker

import com.suspended.music.data.repository.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the most recent media-scan outcome so the background worker can report
 * success/failure to the UI without the ViewModel needing to own the scan.
 */
@Singleton
class ScanStatusStore @Inject constructor() {

    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    fun publish(result: ScanResult) {
        _result.value = result
    }
}
