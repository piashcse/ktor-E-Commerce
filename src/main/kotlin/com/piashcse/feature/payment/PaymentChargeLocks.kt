package com.piashcse.feature.payment

import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.db.isUniqueViolation
import com.piashcse.utils.extension.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Process-local per-order serialization plus DB-backed charge leases. */
internal object PaymentChargeLocks {
    /**
     * Serializes payment creation per order inside this process. The database
     * re-validation still protects against cross-instance races; a distributed
     * lock/advisory lock is the next step for multi-instance deployments.
     */
    private val orderLocks = ConcurrentHashMap<String, Mutex>()
    private val orderLockRefs = ConcurrentHashMap<String, AtomicInteger>()

    suspend fun <T> withLock(
        orderId: String,
        block: suspend () -> T,
    ): T {
        val mutex = orderLocks.computeIfAbsent(orderId) { Mutex() }
        orderLockRefs.computeIfAbsent(orderId) { AtomicInteger() }.incrementAndGet()
        return try {
            mutex.withLock { block() }
        } finally {
            if (orderLockRefs.computeIfAbsent(orderId) { AtomicInteger() }.decrementAndGet() <= 0) {
                orderLocks.remove(orderId, mutex)
                orderLockRefs.remove(orderId)
            }
        }
    }

    suspend fun acquire(
        orderId: String,
        owner: String,
    ): Boolean {
        val existing = query { PaymentChargeLockDAO.find { PaymentChargeLockTable.orderId eq orderId.entityID(OrderTable) }.firstOrNull() }
        if (existing == null) {
            return try {
                query {
                    PaymentChargeLockDAO.new {
                        this.orderId = orderId.entityID(OrderTable)
                        this.owner = owner
                        this.lockedUntil = currentUtc().plusMinutes(5)
                    }
                }
                true
            } catch (e: Exception) {
                if (e.isUniqueViolation()) false else throw e
            }
        }
        if (existing.lockedUntil.isAfter(currentUtc())) return false
        return query {
            PaymentChargeLockTable.update(
                {
                    (PaymentChargeLockTable.orderId eq orderId.entityID(OrderTable)) and
                        (PaymentChargeLockTable.lockedUntil less currentUtc())
                },
            ) {
                it[PaymentChargeLockTable.owner] = owner
                it[PaymentChargeLockTable.lockedUntil] = currentUtc().plusMinutes(5)
            } > 0
        }
    }

    suspend fun release(
        orderId: String,
        owner: String,
    ) {
        query {
            PaymentChargeLockDAO.find {
                (PaymentChargeLockTable.orderId eq orderId.entityID(OrderTable)) and
                    (PaymentChargeLockTable.owner eq owner)
            }.forEach { it.delete() }
        }
    }
}
