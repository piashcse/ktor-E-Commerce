package com.piashcse.feature.shipping

import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentItemDAO
import com.piashcse.database.entities.ShipmentItemTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import org.jetbrains.exposed.v1.core.eq

internal fun ShipmentDAO.toShipmentResponse(items: List<Allocation>? = null): ShipmentResponse {
    val lines =
        items ?: ShipmentItemDAO.find { ShipmentItemTable.shipmentId eq id }
            .map { Allocation(it.orderItemId.value, it.quantity) }
    return ShipmentResponse(
        id = id.value,
        orderId = orderId.value,
        carrier = carrier,
        service = service,
        trackingNumber = trackingNumber,
        labelUrl = labelUrl,
        rateAmount = rateAmount?.let { Money.str(it) },
        currency = currency,
        status = status,
        shippedAt = shippedAt?.toUtcString(),
        deliveredAt = deliveredAt?.toUtcString(),
        createdAt = createdAt.toUtcString(),
        items = lines.map { ShipmentItemResponse(it.orderItemId, it.quantity) },
    )
}
