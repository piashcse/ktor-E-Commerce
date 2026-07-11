package com.piashcse.utils.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Duration.Companion.milliseconds

// ============================================================================
//  DATABASE QUERY HELPERS
// ============================================================================

/** Execute a block within a database transaction on the IO dispatcher. */
suspend fun <T> query(block: () -> T): T =
    withContext(Dispatchers.IO) {
        transaction { block() }
    }

/**
 * Execute a block within a database transaction with retry on failure.
 * Uses exponential backoff: 100ms, 200ms, 400ms between retries.
 * Suitable for write transactions with concurrent access (stock, inventory, coupons).
 */
suspend fun <T> retryQuery(
    maxRetries: Int = 3,
    initialDelayMs: Long = 100,
    block: () -> T,
): T {
    var lastError: Exception? = null
    repeat(maxRetries) { attempt ->
        try {
            return query(block)
        } catch (e: Exception) {
            lastError = e
            if (attempt < maxRetries - 1) {
                delay((initialDelayMs * (1L shl attempt)).milliseconds)
            }
        }
    }
    throw lastError ?: RuntimeException("Retry failed")
}
