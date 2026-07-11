package com.piashcse.service

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.*
import com.piashcse.utils.extension.query
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object StockReservationCleanup {
    private val log = LoggerFactory.getLogger(StockReservationCleanup::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
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

    private suspend fun releaseExpired() = query {
        val expired = StockReservationDAO.find {
            (StockReservationTable.status eq ReservationStatus.ACTIVE) and
                (StockReservationTable.expiresAt less LocalDateTime.now())
        }.toList()

        if (expired.isEmpty()) return@query

        log.info("Releasing ${expired.size} expired reservation(s)")

        expired.map { it.orderId.value }.distinct().forEach { orderId ->
            val order = OrderDAO.findById(orderId) ?: return@forEach
            if (order.status != OrderStatus.PENDING) return@forEach

            val orderItems = OrderItemDAO.find { OrderItemTable.orderId eq order.id }.toList()
            val products = if (orderItems.isNotEmpty())
                ProductDAO.find { ProductTable.id inList orderItems.map { it.productId.value } }
                    .associateBy { it.id.value }
            else emptyMap()

            orderItems.forEach { products[it.productId.value]?.restoreStock(it.quantity) }
            order.status = OrderStatus.CANCELED
            order.canceledDate = LocalDateTime.now()
            order.notes = "Auto-canceled: expired stock reservation"
        }

        expired.forEach { it.status = ReservationStatus.RELEASED }
    }
}
