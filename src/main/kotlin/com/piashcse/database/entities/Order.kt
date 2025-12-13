package com.piashcse.database.entities

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.Order
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object OrderTable : BaseIdTable("order") {
    val userId = reference("user_id", UserTable.id)
    val shopId = reference("shop_id", ShopTable.id).nullable() // Which shop the order belongs to (for multi-vendor)
    val orderNumber = varchar("order_number", 50).uniqueIndex() // Unique order number for tracking
    val subTotal = decimal("sub_total", 10, 2)
    val shippingCost = decimal("shipping_cost", 10, 2).default(BigDecimal("0.00"))
    val taxAmount = decimal("tax_amount", 10, 2).default(BigDecimal("0.00"))
    val discountAmount = decimal("discount_amount", 10, 2).default(BigDecimal("0.00"))
    val total = decimal("total", 10, 2)
    val currency = varchar("currency", 3).default("USD") // Currency of the order
    val paymentMethod = varchar("payment_method", 50).nullable() // Method used for payment
    val paymentStatus = enumerationByName<PaymentStatus>("payment_status", 30).default(PaymentStatus.PENDING)
    val status = enumerationByName<OrderStatus>("status", 30).default(OrderStatus.PENDING)
    val notes = text("notes").nullable() // Additional notes from customer
    val shippingAddress = text("shipping_address").nullable() // Complete shipping address
    val billingAddress = text("billing_address").nullable() // Complete billing address
    val shippingDate = datetime("shipping_date").nullable() // When the order was shipped
    val deliveredDate = datetime("delivered_date").nullable() // When the order was delivered
    val canceledDate = datetime("canceled_date").nullable() // When the order was canceled
    val completedDate = datetime("completed_date").nullable() // When the order was completed
    // createdAt and updatedAt are inherited from BaseIdTable
}

class OrderDAO(id: EntityID<String>) : BaseEntity(id, OrderTable) {
    companion object : BaseEntityClass<OrderDAO>(OrderTable, OrderDAO::class.java)

    var userId by OrderTable.userId
    var shopId by OrderTable.shopId
    var orderNumber by OrderTable.orderNumber
    var subTotal by OrderTable.subTotal
    var shippingCost by OrderTable.shippingCost
    var taxAmount by OrderTable.taxAmount
    var discountAmount by OrderTable.discountAmount
    var total by OrderTable.total
    var currency by OrderTable.currency
    var paymentMethod by OrderTable.paymentMethod
    var paymentStatus by OrderTable.paymentStatus
    var status by OrderTable.status
    var notes by OrderTable.notes
    var shippingAddress by OrderTable.shippingAddress
    var billingAddress by OrderTable.billingAddress
    var shippingDate by OrderTable.shippingDate
    var deliveredDate by OrderTable.deliveredDate
    var canceledDate by OrderTable.canceledDate
    var completedDate by OrderTable.completedDate

    fun response() = Order(
        orderId = id.value,
        subTotal = subTotal.toFloat(),
        total = total.toFloat(),
        status = status
    )
}