package com.piashcse.database.entities

import com.piashcse.constants.RmaResolution
import com.piashcse.constants.RmaStatus
import com.piashcse.constants.RmaType
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/** Return-merchandise authorization (return or exchange). */
object RmaTable : BaseIdTable("rma") {
    val orderId = reference("order_id", OrderTable.id).index()
    val orderItemId = reference("order_item_id", OrderItemTable.id).index()
    val userId = reference("user_id", UserTable.id).index()
    val shopId = reference("shop_id", ShopTable.id).nullable().index()
    val quantity = integer("quantity")
    val type = enumerationByName("type", 20, RmaType::class)
    val status = enumerationByName("status", 20, RmaStatus::class).default(RmaStatus.REQUESTED)
    val reason = text("reason")
    val resolution = enumerationByName("resolution", 20, RmaResolution::class).nullable()
    val trackingNumber = varchar("tracking_number", 100).nullable()
    val replacementOrderId = reference("replacement_order_id", OrderTable.id).nullable()
    val refundRequestId = reference("refund_request_id", RefundRequestTable.id).nullable()
    val resolvedAt = datetime("resolved_at").nullable()
}

class RmaDAO(id: EntityID<String>) : BaseEntity(id, RmaTable) {
    companion object : BaseEntityClass<RmaDAO>(RmaTable, RmaDAO::class.java)

    var orderId by RmaTable.orderId
    var orderItemId by RmaTable.orderItemId
    var userId by RmaTable.userId
    var shopId by RmaTable.shopId
    var quantity by RmaTable.quantity
    var type by RmaTable.type
    var status by RmaTable.status
    var reason by RmaTable.reason
    var resolution by RmaTable.resolution
    var trackingNumber by RmaTable.trackingNumber
    var replacementOrderId by RmaTable.replacementOrderId
    var refundRequestId by RmaTable.refundRequestId
    var resolvedAt by RmaTable.resolvedAt
}
