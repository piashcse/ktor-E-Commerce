package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.feature.order.OrderSearch
import com.piashcse.mapper.toRefundRequestResponse
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.emptyPage
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Read-only refund lookups and enriched list pages. */
internal object RefundQueries {
    suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        userType: UserType,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        readQuery {
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            requireRefundAccess(order.userId.value, order.shopId?.value, userId, userType)

            RefundRequestTable.selectAll()
                .andWhere { RefundRequestTable.orderId eq orderId.entityID(OrderTable) }
                .orderBy(RefundRequestTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
                }.let { enrichRefundPage(it) }
        }

    suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse? =
        readQuery {
            val refundRequest = RefundRequestDAO.findById(refundId) ?: return@readQuery null

            val order = OrderDAO.findById(refundRequest.orderId.value)
            val item = OrderItemDAO.findById(refundRequest.orderItemId.value)
            val customer = UserDAO.findById(refundRequest.userId.value)
            requireRefundAccess(
                order?.userId?.value ?: refundRequest.userId.value,
                order?.shopId?.value,
                userId,
                userType,
            )

            refundRequest.toRefundRequestResponse(
                orderNumber = order?.orderNumber,
                shopId = order?.shopId?.value,
                customerEmail = customer?.email,
                productName = item?.productName,
            )
        }

    suspend fun listForSeller(
        sellerUserId: String,
        status: RefundStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        readQuery {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val orderIds =
                OrderSearch.matchingOrderIdsTx(search, shopId.value)
                    ?: OrderDAO.find { OrderTable.shopId eq shopId }.map { it.id }
            if (orderIds.isEmpty()) {
                return@readQuery emptyPage(limit, offset)
            }
            RefundRequestTable.selectAll()
                .where { RefundRequestTable.orderId inList orderIds }
                .also { q ->
                    status?.let { q.andWhere { RefundRequestTable.status eq it } }
                    q.applyCreatedRange(RefundRequestTable.createdAt, from, to)
                }.orderBy(RefundRequestTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
                }.let { enrichRefundPage(it) }
        }

    suspend fun listAll(
        status: RefundStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        readQuery {
            val matchingOrderIds = OrderSearch.matchingOrderIdsTx(search)
            if (matchingOrderIds != null && matchingOrderIds.isEmpty()) {
                return@readQuery emptyPage(limit, offset)
            }
            RefundRequestTable.selectAll()
                .also { q ->
                    status?.let { q.andWhere { RefundRequestTable.status eq it } }
                    matchingOrderIds?.let { q.andWhere { RefundRequestTable.orderId inList it } }
                    q.applyCreatedRange(RefundRequestTable.createdAt, from, to)
                }.orderBy(RefundRequestTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
                }.let { enrichRefundPage(it) }
        }

    private fun enrichRefundPage(page: PaginatedResponse<RefundRequestResponse>): PaginatedResponse<RefundRequestResponse> {
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
                page.data.map { refund ->
                    val order = orders[refund.orderId]
                    refund.copy(
                        orderNumber = order?.orderNumber,
                        shopId = order?.shopId?.value,
                        customerEmail = users[refund.userId]?.email,
                        productName = items[refund.orderItemId]?.productName,
                    )
                },
        )
    }
}
