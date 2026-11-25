package com.piashcse.database

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Startup leader election for in-process background workers.
 *
 * Each worker holds a Postgres session advisory lock for the lifetime of the
 * process. Only the instance that acquires the lock starts the worker, so
 * multiple API replicas do not run duplicate outbox/billing/cleanup loops.
 *
 * If the leader exits, Postgres releases the lock; a restart (or a later
 * runtime re-election) lets another instance take over.
 */
object DatabaseLeadership {
    private val log = LoggerFactory.getLogger(DatabaseLeadership::class.java)
    private val connections = ConcurrentHashMap<String, Pair<Connection, Long>>()

    fun tryAcquire(
        name: String,
        lockId: Long,
    ): Boolean {
        connections[name]?.let { return !it.first.isClosed }
        return runCatching {
            val connection = getHikariDataSource().connection
            val acquired =
                connection.prepareStatement("SELECT pg_try_advisory_lock(?)").use { statement ->
                    statement.setLong(1, lockId)
                    statement.executeQuery().use { rows -> rows.next() && rows.getBoolean(1) }
                }
            if (acquired) {
                connections[name] = connection to lockId
                log.info("Acquired worker leadership lock '$name' ($lockId)")
                true
            } else {
                connection.close()
                log.info("Worker leadership lock '$name' is held by another instance; worker stays off")
                false
            }
        }.onFailure { log.warn("Could not acquire worker leadership lock '$name'", it) }
            .getOrDefault(false)
    }

    fun release(name: String) {
        connections.remove(name)?.let { (connection, lockId) ->
            runCatching {
                connection.prepareStatement("SELECT pg_advisory_unlock(?)").use { statement ->
                    statement.setLong(1, lockId)
                    statement.executeQuery().close()
                }
            }.onFailure { log.warn("Failed to release worker leadership lock '$name'", it) }
            runCatching { connection.close() }
        }
    }

    fun releaseAll() {
        connections.keys.toList().forEach(::release)
    }
}
