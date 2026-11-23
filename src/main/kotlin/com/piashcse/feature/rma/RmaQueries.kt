package com.piashcse.feature.rma

import com.piashcse.constants.Message
import com.piashcse.constants.RmaStatus
import com.piashcse.database.entities.*
import com.piashcse.feature.order.OrderSearch
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Read-only RMA lists and enrichment. */
internal object RmaQueries {
    suspend fun getUserRmas(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RmaResponse> =
        query {
            RmaTable.selectAll().andWhere { RmaTable.userId eq userId }
                .orderBy(RmaTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { RmaDAO.wrapRow(it).toRmaResponse() }
                .let { enrichRmas(it) }
        }

    suspend fun getSellerRmas(
        sellerUserId: String,
        status: RmaStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RmaResponse> =
        readQuery {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException(Message.Orders.NO_SHOP_ASSOCIATED)
            val orderIds =
                OrderSearch.matchingOrderIdsTx(search, shopId)
                    ?: OrderDAO.find { OrderTable.shopId eq shopId }.map { it.id }
            if (orderIds.isEmpty()) return@readQuery com.piashcse.utils.common.emptyPage(limit, offset)
            RmaTable.selectAll().andWhere { RmaTable.orderId inList orderIds }
                .also { q ->
                    status?.let { q.andWhere { RmaTable.status eq it } }
                    q.applyCreatedRange(RmaTable.createdAt, from, to)
                }
                .orderBy(RmaTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { RmaDAO.wrapRow(it).toRmaResponse() }
                .let { enrichRmas(it) }
        }

    suspend fun getAdminRmas(
        limit: Int,
        offset: Int,
        status: RmaStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
    ): PaginatedResponse<RmaResponse> =
        readQuery {
            val matchingOrderIds = OrderSearch.matchingOrderIdsTx(search)
            if (matchingOrderIds != null && matchingOrderIds.isEmpty()) {
                return@readQuery com.piashcse.utils.common.emptyPage(limit, offset)
            }
            val query = RmaTable.selectAll()
            status?.let { query.andWhere { RmaTable.status eq it } }
            matchingOrderIds?.let { query.andWhere { RmaTable.orderId inList it } }
            query.applyCreatedRange(RmaTable.createdAt, from, to)
            query.orderBy(RmaTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { RmaDAO.wrapRow(it).toRmaResponse() }
                .let { enrichRmas(it) }
        }

    private fun enrichRmas(page: PaginatedResponse<RmaResponse>): PaginatedResponse<RmaResponse> {
        if (page.data.isEmpty()) return page
        val orders =
            OrderDAO.find { OrderTable.id inList page.data.map { it.orderId }.distinct().map { it.entityID(OrderTable) } }
                .associateBy { it.id.value }
        val items =
            OrderItemDAO.find {
                OrderItemTable.id inList page.data.map { it.orderItemId }.distinct().map { it.entityID(OrderItemTable) }
            }.associateBy { it.id.value }
        val users =
            UserDAO.find { UserTable.id inList page.data.map { it.userId }.distinct().map { it.entityID(UserTable) } }
                .associateBy { it.id.value }
        return page.copy(
            data =
                page.data.map { rma ->
                    rma.copy(
                        orderNumber = orders[rma.orderId]?.orderNumber,
                        customerEmail = users[rma.userId]?.email,
                        productName = items[rma.orderItemId]?.productName,
                    )
                },
        )
    }

    suspend fun ship(
        rmaId: String,
        userId: String,
        trackingNumber: String,
    ): RmaResponse = RmaLifecycleService.ship(rmaId, userId, trackingNumber)

    suspend fun cancel(
        rmaId: String,
        userId: String,
    ): RmaResponse = RmaLifecycleService.cancel(rmaId, userId)
}
