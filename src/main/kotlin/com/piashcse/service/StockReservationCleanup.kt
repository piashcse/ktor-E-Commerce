package com.piashcse.service

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ReservationStatus
import com.piashcse.database.entities.StockReservationDAO
import com.piashcse.database.entities.StockReservationTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.order.OrderCancellation
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.slf4j.LoggerFactory

object StockReservationCleanup {
    private val log = LoggerFactory.getLogger(StockReservationCleanup::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job =
            scope.launch {
                while (isActive) {
                    runCatching { releaseExpired() }
                        .onFailure { log.error("Cleanup failed", it) }
                    delay(15 * 60 * 1000L)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun releaseExpired() =
        query {
            // Identify candidates first, then lock the order before its reservations.
            // This matches the cancellation lock order and avoids deadlocks.
            val candidates =
                StockReservationDAO.find {
                    (StockReservationTable.status eq ReservationStatus.ACTIVE) and
                        (StockReservationTable.expiresAt less currentUtc())
                }.map { it.orderId.value }
                    .toSet()

            if (candidates.isEmpty()) return@query

            candidates.forEach { orderId ->
                val order =
                    OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: return@forEach
                val reservations =
                    StockReservationDAO.find {
                        (StockReservationTable.orderId eq order.id) and
                            (StockReservationTable.status eq ReservationStatus.ACTIVE) and
                            (StockReservationTable.expiresAt less currentUtc())
                    }.forUpdate().toList()
                if (reservations.isEmpty()) return@forEach

                log.info("Releasing ${reservations.size} expired reservation(s)")
                when {
                    order.paymentStatus == PaymentStatus.COMPLETED -> {
                        // Paid order: stock is legitimately consumed; keep the reservation finalized.
                        reservations.forEach { it.status = ReservationStatus.FINALIZED }
                    }
                    order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED -> {
                        // Shared cancellation path restores stock, sales, seller totals,
                        // coupon usage, payout rows, history and marks reservations RELEASED.
                        OrderCancellation.cancel(
                            order,
                            "Auto-canceled: expired stock reservation",
                            changedBy = null,
                        )
                    }
                    else -> {
                        // Non-cancellable order (already shipped/delivered): do not touch stock.
                        reservations.forEach { it.status = ReservationStatus.FINALIZED }
                    }
                }
            }
        }
}
