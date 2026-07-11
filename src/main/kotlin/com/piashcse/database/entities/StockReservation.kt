package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

enum class ReservationStatus {
    ACTIVE, FINALIZED, RELEASED
}

object StockReservationTable : BaseIdTable("stock_reservation") {
    val orderId = reference("order_id", OrderTable.id).index()
    val orderItemId = reference("order_item_id", OrderItemTable.id)
    val productId = reference("product_id", ProductTable.id)
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val quantity = integer("quantity")
    val status = enumerationByName<ReservationStatus>("status", 20).default(ReservationStatus.ACTIVE)
    val expiresAt = datetime("expires_at")

    init {
        index(customIndexName = "stock_reservation_status_idx", isUnique = false, status)
        index(customIndexName = "stock_reservation_expires_at_idx", isUnique = false, expiresAt)
    }
}

class StockReservationDAO(id: EntityID<String>) : BaseEntity(id, StockReservationTable) {
    companion object : BaseEntityClass<StockReservationDAO>(StockReservationTable, StockReservationDAO::class.java)

    var orderId by StockReservationTable.orderId
    var orderItemId by StockReservationTable.orderItemId
    var productId by StockReservationTable.productId
    var shopId by StockReservationTable.shopId
    var quantity by StockReservationTable.quantity
    var status by StockReservationTable.status
    var expiresAt by StockReservationTable.expiresAt
}
