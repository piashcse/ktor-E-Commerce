package com.piashcse.utils.extension

import com.piashcse.database.getReadDatabase
import com.piashcse.utils.validator.AppException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.valiktor.ConstraintViolationException
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
 * Read-only query. Uses the configured replica when `DB_READ_URL` is set,
 * otherwise falls back to the primary database. Callers must not write here.
 */
suspend fun <T> readQuery(block: () -> T): T =
    withContext(Dispatchers.IO) {
        val replica = getReadDatabase()
        if (replica != null) transaction(replica) { block() } else transaction { block() }
    }

/**
 * Execute a block within a database transaction with retry on failure.
 * Uses exponential backoff: 100ms, 200ms, 400ms between retries.
 * Suitable for write transactions with concurrent access (stock, inventory, coupons).
 * Only transient DB errors are retried — validation / not-found / conflict errors fail fast.
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
        } catch (e: AppException) {
            throw e
        } catch (e: ConstraintViolationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            // Fail fast for business errors — retrying hides bugs and amplifies coupon/stock races.
            lastError = e
            if (attempt < maxRetries - 1) {
                delay((initialDelayMs * (1L shl attempt)).milliseconds)
            }
        }
    }
    throw lastError ?: RuntimeException("Retry failed")
}
