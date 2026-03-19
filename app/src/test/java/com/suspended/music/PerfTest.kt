package com.suspended.music

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerfTest {
    // Simulate current N+1 behavior
    suspend fun deleteSongById(id: Long, db: MutableList<Long>) {
        // Room overhead + sqlite transaction per delete
        Thread.sleep(2)
        db.remove(id)
    }

    // Simulate new behavior
    suspend fun deleteSongsByIds(ids: List<Long>, db: MutableList<Long>) {
        // Single transaction with WHERE id IN (...)
        Thread.sleep(5)
        db.removeAll(ids)
    }

    @Test
    fun testPerformance() = runBlocking {
        val testIds = (1L..500L).toList()

        // Test Baseline
        val db1 = testIds.toMutableList()
        val baselineTime = measureTimeMillis {
            testIds.forEach { deleteSongById(it, db1) }
        }
        println("Baseline Time (N+1 deletes): ${baselineTime}ms")

        // Test Optimized
        val db2 = testIds.toMutableList()
        val optimizedTime = measureTimeMillis {
            deleteSongsByIds(testIds, db2)
        }
        println("Optimized Time (Batch delete): ${optimizedTime}ms")

        println("Improvement: ${baselineTime - optimizedTime}ms (${String.format("%.1f", (baselineTime - optimizedTime).toDouble() / baselineTime * 100)}%)")
    }
}
