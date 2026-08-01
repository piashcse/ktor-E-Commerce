package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.database.entities.*
import com.piashcse.mapper.toRefundRequestResponse
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime
import java.time.ZoneOffset

class RefundRequestRepositoryImpl : RefundRequestRepository {

    override suspend fun getRefundAccess(
        userId: String,
        refundId: String,
    ): RefundAccess = query {
        val refundReq =
            RefundRequestDAO.findById(refundId)
                ?: refundId.throwNotFound("Refund request")

        val user =
            UserDAO.findById(userId)
                ?: userId.throwNotFound("User")

        val orderItem =
            OrderItemDAO.findById(refundReq.orderItemId.value)
                ?: refundReq.orderItemId.value.throwNotFound("Order item")

        RefundAccess(
            refundId = refundReq.id.value,
            orderId = refundReq.orderId.value,
            orderItemId = refundReq.orderItemId.value,
            currentStatus = refundReq.status,
            currentRefundAmount = refundReq.refundAmount,
            maxRefundAmount = orderItem.total,
            isCustomer = refundReq.userId.value == userId,
            isSeller = orderBelongsToUserShop(refundReq.orderId.value, userId),
            isAdmin = user.userType.isAdminOrHigher,
        )
    }

    override suspend fun getRefundOrderAccess(
        userId: String,
        orderId: String,
    ): RefundOrderAccess = query {
        val order =
            OrderDAO.findById(orderId)
                ?: orderId.throwNotFound("Order")

        val user =
            UserDAO.findById(userId)
                ?: userId.throwNotFound("User")

        RefundOrderAccess(
            orderId = order.id.value,
            isCustomer = order.userId.value == userId,
            isSeller = orderBelongsToUserShop(orderId, userId),
            isAdmin = user.userType.isAdminOrHigher,
        )
    }

    private fun orderBelongsToUserShop(
        orderId: String,
        userId: String,
    ): Boolean {
        val order = OrderDAO.findById(orderId) ?: return false
        val shopId = order.shopId?.value ?: return false
        return sellerOwnsShop(userId, shopId)
    }

    override suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse = query {
        val orderItem =
            OrderItemDAO.find {
                (OrderItemTable.id eq request.orderItemId.entityID(OrderItemTable)) and
                    (OrderItemTable.orderId eq orderId.entityID(OrderTable))
            }.firstOrNull() ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)

        val existingRefund =
            RefundRequestDAO.find {
                (RefundRequestTable.orderItemId eq orderItem.id) and
                    (RefundRequestTable.status inList listOf(RefundStatus.PENDING, RefundStatus.APPROVED, RefundStatus.SHIPPED))
            }.firstOrNull()

        if (existingRefund != null) {
            throw ValidationException(Message.Refunds.ALREADY_EXISTS)
        }

        val refundRequest =
            RefundRequestDAO.new {
                this.orderItemId = orderItem.id
                this.userId = userId.entityID(UserTable)
                this.orderId = orderId.entityID(OrderTable)
                this.reason = request.reason
                this.images = request.images
                this.status = RefundStatus.PENDING
            }

        refundRequest.toRefundRequestResponse()
    }

    override suspend fun getRefundsByOrderId(
        orderId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> = query {
        RefundRequestTable.selectAll()
            .andWhere { RefundRequestTable.orderId eq orderId.entityID(OrderTable) }
            .toPaginatedResponse(limit, offset) {
                RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
            }
    }

    override suspend fun getRefundById(refundId: String): RefundRequestResponse? = query {
        RefundRequestDAO.findById(refundId)?.toRefundRequestResponse()
    }

    override suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
    ): RefundRequestResponse = query {
        val refundReq =
            RefundRequestDAO.findById(refundId)
                ?: refundId.throwNotFound("Refund request")

        refundReq.status = request.status
        refundReq.resolvedAt = LocalDateTime.now(ZoneOffset.UTC)

        request.refundAmount?.let { refundReq.refundAmount = it }
        request.refundMethod?.let { refundReq.refundMethod = it }

        if (request.status == RefundStatus.REFUNDED) {
            val order =
                OrderDAO.findById(refundReq.orderId.value)
                    ?: refundReq.orderId.value.throwNotFound("Order")
            order.paymentStatus = PaymentStatus.REFUNDED
        }

        refundReq.toRefundRequestResponse()
    }

    override suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
    ): RefundRequestResponse = query {
        val refundReq =
            RefundRequestDAO.findById(refundId)
                ?: refundId.throwNotFound("Refund request")

        refundReq.trackingNumber = request.trackingNumber
        refundReq.status = RefundStatus.SHIPPED

        refundReq.toRefundRequestResponse()
    }
}
