package com.piashcse.utils.extension

import com.piashcse.event.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Duration.Companion.milliseconds

// ============================================================================
//  DATABASE QUERY HELPERS
// ============================================================================

/**
 * Execute a block within a database transaction on the IO dispatcher.
 * Domain events published inside the block are emitted only after the
 * transaction commits successfully (discarded on rollback).
 */
suspend fun <T> query(block: () -> T): T =
    withContext(Dispatchers.IO) {
        EventBus.beginAfterCommitScope()
        try {
            val result = transaction { block() }
            EventBus.commitAfterCommitScope()
            result
        } catch (e: Exception) {
            EventBus.abortAfterCommitScope()
            throw e
        }
    }

/**
 * Execute a block within a database transaction with retry on failure.
 * Uses exponential backoff: 100ms, 200ms, 400ms between retries.
 * Each retry re-runs the whole block (which may call several repository
 * methods that join the surrounding transaction).
 */
suspend fun <T> suspendRetryQuery(
    maxRetries: Int = 3,
    initialDelayMs: Long = 100,
    block: suspend () -> T,
): T {
    var lastError: Exception? = null
    repeat(maxRetries) { attempt ->
        try {
            return withContext(Dispatchers.IO) {
                EventBus.beginAfterCommitScope()
                try {
                    val result = suspendTransaction { block() }
                    EventBus.commitAfterCommitScope()
                    result
                } catch (e: Exception) {
                    EventBus.abortAfterCommitScope()
                    throw e
                }
            }
        } catch (e: Exception) {
            lastError = e
            if (attempt < maxRetries - 1) {
                delay((initialDelayMs * (1L shl attempt)).milliseconds)
            }
        }
    }
    throw lastError ?: RuntimeException("Retry failed")
}
