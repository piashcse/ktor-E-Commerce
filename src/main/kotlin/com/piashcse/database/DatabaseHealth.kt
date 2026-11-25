package com.piashcse.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Readiness check used by the HTTP edge. Kept here so route files stay free of
 * persistence imports.
 */
suspend fun isDatabaseReachable(): Boolean =
    runCatching {
        withTimeout(3_000) {
            withContext(Dispatchers.IO) {
                transaction {
                    exec("SELECT 1")
                }
                // Reads are routed to the replica when configured; readiness must
                // fail if it is unreachable.
                getReadDatabase()?.let { replica ->
                    transaction(replica) { exec("SELECT 1") }
                }
            }
        }
    }.isSuccess
