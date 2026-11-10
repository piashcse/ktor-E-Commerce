package com.piashcse.database.entities

import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object RefundRequestTable : BaseIdTable("refund_request") {
    val orderItemId = reference("order_item_id", OrderItemTable.id)
    val userId = reference("user_id", UserTable.id)
    val orderId = reference("order_id", OrderTable.id)
    val reason = text("reason")
    val images = varchar("images", 2000).nullable()
    val status = enumerationByName("status", 20, RefundStatus::class).default(RefundStatus.PENDING)
    val currency = varchar("currency", 3).default("USD")
    val quantity = integer("quantity").default(1)
    val refundAmount = decimal("refund_amount", 10, 2).nullable()
    val shippingRefund = decimal("shipping_refund", 10, 2).default(java.math.BigDecimal("0.00"))
    val taxRefund = decimal("tax_refund", 10, 2).default(java.math.BigDecimal("0.00"))
    val refundMethod = enumerationByName("refund_method", 50, RefundMethod::class).nullable()
    val trackingNumber = varchar("tracking_number", 100).nullable()
    val requestedAt = datetime("requested_at").clientDefault { currentUtc() }
    val resolvedAt = datetime("resolved_at").nullable()
    // createdAt and updatedAt are inherited from BaseIdTable
}

class RefundRequestDAO(id: EntityID<String>) : BaseEntity(id, RefundRequestTable) {
    companion object : BaseEntityClass<RefundRequestDAO>(RefundRequestTable, RefundRequestDAO::class.java)

    var orderItemId by RefundRequestTable.orderItemId
    var userId by RefundRequestTable.userId
    var orderId by RefundRequestTable.orderId
    var reason by RefundRequestTable.reason
    var images by RefundRequestTable.images
    var status by RefundRequestTable.status
    var currency by RefundRequestTable.currency
    var quantity by RefundRequestTable.quantity
    var refundAmount by RefundRequestTable.refundAmount
    var shippingRefund by RefundRequestTable.shippingRefund
    var taxRefund by RefundRequestTable.taxRefund
    var refundMethod by RefundRequestTable.refundMethod
    var trackingNumber by RefundRequestTable.trackingNumber
    var requestedAt by RefundRequestTable.requestedAt
    var resolvedAt by RefundRequestTable.resolvedAt
}
