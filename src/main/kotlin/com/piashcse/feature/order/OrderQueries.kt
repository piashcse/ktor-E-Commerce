package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.mapper.toOrderResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.CursorPage
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.common.cursorPage
import com.piashcse.utils.extension.applyCreatedRange
import com.piashcse.utils.extension.escapeLike
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedList
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

/**
 * Read-only order projections (customer, guest, seller, admin and cursor).
 * Kept separate from [OrderRepositoryImpl] so the write/checkout lifecycle can
 * evolve without carrying the list/detail query surface.
 */
object OrderQueries {
    suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        search: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> =
        readQuery {
            val orders = OrderTable.selectAll().andWhere { OrderTable.userId eq userId }
            status?.let { orders.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            search?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase().escapeLike()}%"
                orders.andWhere {
                    (OrderTable.orderNumber.lowerCase() like pattern) or
                        (OrderTable.contactEmail.lowerCase() like pattern)
                }
            }
            orders.applyCreatedRange(OrderTable.createdAt, from, to)
            orders
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset, forCustomer = true)
        }

    suspend fun getOrdersCursor(
        userId: String,
        limit: Int,
        cursor: String?,
    ): CursorPage<OrderResponse> =
        readQuery {
            val page =
                OrderTable.selectAll()
                    .andWhere { OrderTable.userId eq userId }
                    .cursorPage(limit, cursor, OrderTable.createdAt, OrderTable.id) { it }
            val orders = page.data.map { OrderDAO.wrapRow(it) }
            val itemsMap = loadItemsForOrders(orders)
            CursorPage(
                data = orders.map { it.toOrderResponse(itemsMap[it.id.value], forCustomer = true) },
                nextCursor = page.nextCursor,
                hasMore = page.hasMore,
                limit = page.limit,
            )
        }

    suspend fun getOrderForGuest(
        orderNumber: String,
        email: String,
    ): OrderResponse? =
        readQuery {
            val order =
                OrderDAO.find {
                    (OrderTable.orderNumber eq orderNumber) and
                        (OrderTable.contactEmail.lowerCase() eq email.trim().lowercase())
                }.firstOrNull() ?: return@readQuery null
            order.toOrderResponse(
                OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                includeSensitive = false,
                forCustomer = true,
            )
        }

    suspend fun getOrderById(
        orderId: String,
        userId: String,
        userType: UserType,
    ): OrderResponse =
        readQuery {
            orderId.requireNotBlank("Order ID")
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            requireOrderAccess(order.userId.value, order.shopId?.value, userId, userType)
            order.toOrderResponse(
                OrderItemDAO.itemsForOrder(order.id).toItemResponses(),
                includeSensitive = userType == UserType.CUSTOMER,
                forCustomer = userType == UserType.CUSTOMER,
            )
        }

    suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
        search: String?,
        from: Instant?,
        to: Instant?,
    ): PaginatedResponse<OrderResponse> =
        readQuery {
            val seller =
                try {
                    requireSellerByUserId(userId)
                } catch (_: ForbiddenException) {
                    throw ValidationException(Message.Orders.SELLER_PROFILE_NOT_FOUND)
                }
            val shopId = seller.shopId ?: throw ValidationException(Message.Orders.NO_SHOP_ASSOCIATED)

            val orders = OrderTable.selectAll().andWhere { OrderTable.shopId eq shopId }
            status?.let { orders.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            search?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase().escapeLike()}%"
                orders.andWhere {
                    (OrderTable.orderNumber.lowerCase() like pattern) or
                        (OrderTable.contactEmail.lowerCase() like pattern)
                }
            }
            orders.applyCreatedRange(OrderTable.createdAt, from, to)
            orders
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset, includeSensitive = false)
        }

    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        search: String?,
    ): PaginatedResponse<OrderResponse> =
        readQuery {
            val orders = OrderTable.selectAll()
            status?.let { orders.andWhere { OrderTable.status eq parseOrderStatus(it) } }
            orders.applyCreatedRange(OrderTable.createdAt, startDate, endDate)
            search?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase().escapeLike()}%"
                orders.andWhere {
                    (OrderTable.orderNumber.lowerCase() like pattern) or
                        (OrderTable.contactEmail.lowerCase() like pattern)
                }
            }
            orders
                .orderBy(OrderTable.createdAt to SortOrder.DESC)
                .toOrdersPaginated(limit, offset, includeSensitive = false)
        }

    private fun Query.toOrdersPaginated(
        limit: Int,
        offset: Int,
        includeSensitive: Boolean = true,
        forCustomer: Boolean = false,
    ): PaginatedResponse<OrderResponse> {
        val (totalCount, rows) = toPaginatedList(limit, offset) { OrderDAO.wrapRow(it) }
        val itemsMap = loadItemsForOrders(rows)
        val data =
            rows.map { order ->
                order.toOrderResponse(
                    itemsMap[order.id.value],
                    includeSensitive = includeSensitive,
                    forCustomer = forCustomer,
                )
            }
        return PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
    }

    private fun parseOrderStatus(raw: String): OrderStatus =
        runCatching { OrderStatus.valueOf(raw.trim().uppercase()) }.getOrElse {
            throw ValidationException(Message.Errors.invalidParameter("status", raw))
        }
}
