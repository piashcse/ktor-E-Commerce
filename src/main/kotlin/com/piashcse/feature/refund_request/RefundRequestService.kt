package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.PaginatedResponse
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime

interface RefundRequestRepository {
    suspend fun createRefundRequest(userId: String, orderId: String, request: RefundRequestRequest): RefundRequestResponse
    suspend fun getRefundsByOrderId(orderId: String, userId: String, userType: UserType, limit: Int = 20, offset: Int = 0): PaginatedResponse<RefundRequestResponse>
    suspend fun getRefundById(refundId: String): RefundRequestResponse?
    suspend fun updateRefundStatus(refundId: String, request: UpdateRefundStatusRequest, userId: String): RefundRequestResponse
    suspend fun shipRefund(refundId: String, request: ShipRefundRequest, userId: String): RefundRequestResponse
}

class RefundRequestService : RefundRequestRepository {

    override suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest
    ): RefundRequestResponse = query {
        request.validation()

        // Verify the order belongs to the user
        val order = OrderDAO.findById(orderId)
            ?: throw ValidationException(Message.Orders.NOT_FOUND)

        if (order.userId.value != userId) {
            throw ValidationException(Message.Orders.UNAUTHORIZED)
        }

        // Verify the order item exists and belongs to the order
        val orderItem = OrderItemDAO.find {
            (OrderItemTable.id eq EntityID(request.orderItemId, OrderItemTable)) and
            (OrderItemTable.orderId eq EntityID(orderId, OrderTable))
        }.firstOrNull() ?: throw ValidationException("Order item not found")

        // Check if refund already exists for this item
        val existingRefund = RefundRequestDAO.find {
            (RefundRequestTable.orderItemId eq orderItem.id) and
            (RefundRequestTable.status eq "PENDING")
        }.firstOrNull()

        if (existingRefund != null) {
            throw ValidationException("Refund request already exists for this item")
        }

        // Create refund request
        val refundRequest = RefundRequestDAO.new {
            this.orderItemId = orderItem.id
            this.userId = EntityID(userId, UserTable)
            this.orderId = EntityID(orderId, OrderTable)
            this.reason = request.reason
            this.images = request.images
            this.status = "PENDING"
        }

        refundRequest.response()
    }

    override suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        userType: UserType,
        limit: Int,
        offset: Int
    ): PaginatedResponse<RefundRequestResponse> = query {
        val order = OrderDAO.findById(orderId)
            ?: throw ValidationException(Message.Orders.NOT_FOUND)

        // Permission check
        val isCustomer = order.userId.value == userId
        val isSeller = order.shopId?.value?.let { shopId ->
            SellerDAO.find { SellerTable.userId eq userId }.singleOrNull()?.shopId?.value == shopId
        } == true
        val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)

        if (!isCustomer && !isSeller && !isAdmin) {
            throw ValidationException(Message.Orders.UNAUTHORIZED)
        }

        RefundRequestTable.selectAll()
            .andWhere { RefundRequestTable.orderId eq EntityID(orderId, OrderTable) }
            .toPaginatedResponse(limit, offset) {
                RefundRequestDAO.wrapRow(it).response()
            }
    }

    override suspend fun getRefundById(refundId: String): RefundRequestResponse? = query {
        RefundRequestDAO.findById(refundId)?.response()
    }

    override suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String
    ): RefundRequestResponse = query {
        request.validation()

        val refundReq = RefundRequestDAO.findById(refundId)
            ?: throw ValidationException("Refund request not found")

        // Verify user is seller or admin
        val user = UserDAO.findById(userId)
            ?: throw ValidationException(Message.Errors.NOT_FOUND)

        val isAdmin = user.userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
        val isSeller = SellerDAO.find { SellerTable.userId eq userId }.firstOrNull() != null

        if (!isAdmin && !isSeller) {
            throw ValidationException(Message.Errors.FORBIDDEN)
        }

        val validStatuses = listOf("APPROVED", "REJECTED", "REFUNDED")
        if (request.status !in validStatuses) {
            throw ValidationException("Invalid status. Must be one of: ${validStatuses.joinToString(", ")}")
        }

        refundReq.status = request.status
        refundReq.resolvedAt = LocalDateTime.now(java.time.ZoneOffset.UTC)

        if (request.refundAmount != null) {
            refundReq.refundAmount = java.math.BigDecimal.valueOf(request.refundAmount)
        }
        if (request.refundMethod != null) {
            refundReq.refundMethod = request.refundMethod
        }

        refundReq.response()
    }

    override suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String
    ): RefundRequestResponse = query {
        request.validation()

        val refundReq = RefundRequestDAO.findById(refundId)
            ?: throw ValidationException("Refund request not found")

        // Only the customer who owns the order can mark as shipped
        if (refundReq.userId.value != userId) {
            throw ValidationException(Message.Orders.UNAUTHORIZED)
        }

        if (refundReq.status != "APPROVED") {
            throw ValidationException("Refund must be approved before shipping")
        }

        refundReq.trackingNumber = request.trackingNumber
        refundReq.status = "SHIPPED"

        refundReq.response()
    }
}
