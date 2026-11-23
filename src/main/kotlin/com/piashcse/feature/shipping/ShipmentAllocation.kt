package com.piashcse.feature.shipping

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentItemDAO
import com.piashcse.database.entities.ShipmentItemTable
import com.piashcse.database.entities.ShipmentTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInList

internal data class Allocation(
    val orderItemId: String,
    val quantity: Int,
)

/** Existing-shipment validation and stock allocation planning. */
internal object ShipmentAllocation {
    fun planTx(
        orderId: String,
        userId: String,
        requested: List<ShipmentItemRequest>,
    ): List<Allocation> {
        // Lock the order before reading shipment quantities so concurrent label
        // purchases cannot allocate the same units.
        val order =
            OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                .forUpdate()
                .firstOrNull()
                ?: orderId.throwNotFound("Order")
        val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
        if (user.userType !in listOf(UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN)) {
            throw ForbiddenException("Only sellers and admins can purchase shipping labels")
        }
        requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
        if (order.status !in listOf(OrderStatus.PAID, OrderStatus.SHIPPED)) {
            throw ValidationException("Order must be paid before shipping labels can be purchased")
        }
        val items = OrderItemDAO.itemsForOrder(order.id)
        if (items.isEmpty()) throw ValidationException("Order has no items to ship")
        val shipped = shippedQuantities(order.id)
        val remaining =
            items.associate { item ->
                item.id.value to (item.quantity - (shipped[item.id.value] ?: 0))
            }
        val allocation =
            if (requested.isEmpty()) {
                remaining.filterValues { it > 0 }.map { Allocation(it.key, it.value) }
            } else {
                // Merge duplicate lines first; two 5-unit lines against 5
                // remaining must fail before a label is purchased.
                val merged = requested.groupBy { it.orderItemId }.map { (id, lines) -> id to lines.sumOf { it.quantity } }
                merged.map { (orderItemId, quantity) ->
                    if (quantity <= 0) throw ValidationException("Shipment quantity must be positive")
                    val available =
                        remaining[orderItemId]
                            ?: throw ValidationException("Order item $orderItemId is not part of this order")
                    if (quantity > available) {
                        throw ValidationException(
                            "Cannot ship $quantity of $orderItemId; only $available remaining",
                        )
                    }
                    Allocation(orderItemId, quantity)
                }
            }
        if (allocation.isEmpty()) throw ValidationException("All order items have already shipped")
        return allocation
    }

    private fun shippedQuantities(orderId: org.jetbrains.exposed.v1.core.dao.id.EntityID<String>): Map<String, Int> {
        val shipmentIds =
            ShipmentDAO.find {
                (ShipmentTable.orderId eq orderId) and
                    (ShipmentTable.status notInList listOf(ShipmentStatus.CANCELED, ShipmentStatus.FAILED))
            }.map { it.id.value }
        if (shipmentIds.isEmpty()) return emptyMap()
        return ShipmentItemDAO.find { ShipmentItemTable.shipmentId inList shipmentIds }
            .groupBy { it.orderItemId.value }
            .mapValues { (_, rows) -> rows.sumOf { it.quantity } }
    }

    fun allUnitsShipped(order: OrderDAO): Boolean {
        val items = OrderItemDAO.itemsForOrder(order.id)
        val shipped = shippedQuantities(order.id)
        return items.all { (shipped[it.id.value] ?: 0) >= it.quantity }
    }
}
