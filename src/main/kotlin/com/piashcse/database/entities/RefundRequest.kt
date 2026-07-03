package com.piashcse.database.entities

import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object RefundRequestTable : BaseIdTable("refund_request") {
    val orderItemId = reference("order_item_id", OrderItemTable.id)
    val userId = reference("user_id", UserTable.id)
    val orderId = reference("order_id", OrderTable.id)
    val reason = text("reason")
    val images = varchar("images", 2000).nullable()
    val status = enumerationByName("status", 20, RefundStatus::class).default(RefundStatus.PENDING)
    val refundAmount = decimal("refund_amount", 10, 2).nullable()
    val refundMethod = enumerationByName("refund_method", 50, RefundMethod::class).nullable()
    val trackingNumber = varchar("tracking_number", 100).nullable()
    val requestedAt = datetime("requested_at").clientDefault { LocalDateTime.now(ZoneOffset.UTC) }
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
    var refundAmount by RefundRequestTable.refundAmount
    var refundMethod by RefundRequestTable.refundMethod
    var trackingNumber by RefundRequestTable.trackingNumber
    var requestedAt by RefundRequestTable.requestedAt
    var resolvedAt by RefundRequestTable.resolvedAt

    fun response() =
        com.piashcse.model.response.RefundRequestResponse(
            id = id.value,
            orderItemId = orderItemId.value,
            orderId = orderId.value,
            userId = userId.value,
            reason = reason,
            images = images,
            status = status,
            refundAmount = refundAmount?.toPlainString(),
            refundMethod = refundMethod,
            trackingNumber = trackingNumber,
            requestedAt = requestedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            resolvedAt = resolvedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            updatedAt = updatedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "",
        )
}
