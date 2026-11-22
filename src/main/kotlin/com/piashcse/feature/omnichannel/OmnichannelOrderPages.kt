package com.piashcse.feature.omnichannel

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.mapper.toOrderItemResponse
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.paginateWithPreload
import org.jetbrains.exposed.v1.jdbc.Query

/** Batched order page used by pickup and POS order lists. */
internal fun Query.toPosOrdersPaginated(
    limit: Int,
    offset: Int,
): PaginatedResponse<OrderResponse> =
    paginateWithPreload(
        limit,
        offset,
        rowMapper = { OrderDAO.wrapRow(it) },
        preloadAndMap = { orders ->
            val items =
                OrderItemDAO.itemsForOrders(orders.map { it.id })
                    .mapValues { (_, rows) -> rows.map { it.toOrderItemResponse() } }
            orders.map { it.toOrderResponse(items[it.id.value], includeSensitive = false) }
        },
    )
