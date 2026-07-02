package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

interface RefundRequestRepository {
    suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse

    suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        userType: UserType,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<RefundRequestResponse>

    suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse?

    suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse

    suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse
}

class RefundRequestService : RefundRequestRepository {
    override suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse =
        query {
            // Verify the order belongs to the user
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            if (order.userId.value != userId) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            // Verify the order item exists and belongs to the order
            val orderItem =
                OrderItemDAO.find {
                    (OrderItemTable.id eq EntityID(request.orderItemId, OrderItemTable)) and
                        (OrderItemTable.orderId eq EntityID(orderId, OrderTable))
                }.firstOrNull() ?: throw ValidationException("Order item not found")

            // Check if refund already exists for this item
            val existingRefund =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderItemId eq orderItem.id) and
                        (RefundRequestTable.status eq "PENDING")
                }.firstOrNull()

            if (existingRefund != null) {
                throw ValidationException("Refund request already exists for this item")
            }

            // Create refund request
            val refundRequest =
                RefundRequestDAO.new {
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
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        query {
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            // Permission check
            val isCustomer = order.userId.value == userId
            val isSeller =
                order.shopId?.value?.let { shopId ->
                    sellerOwnsShop(userId, shopId)
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

    override suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse? =
        query {
            val refundRequest = RefundRequestDAO.findById(refundId) ?: return@query null

            val isCustomer = refundRequest.userId.value == userId
            val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
            val isSeller = orderBelongsToUserShop(refundRequest.orderId.value, userId)

            if (!isCustomer && !isSeller && !isAdmin) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            refundRequest.response()
        }

    private fun orderBelongsToUserShop(
        orderId: String,
        userId: String,
    ): Boolean {
        val order = OrderDAO.findById(orderId) ?: return false
        val shopId = order.shopId?.value ?: return false
        val seller = SellerDAO.find {
            (SellerTable.userId eq userId) and (SellerTable.shopId eq EntityID(shopId, ShopTable))
        }.firstOrNull()
        return seller != null
    }

    override suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse =
        query {
            val refundReq =
                RefundRequestDAO.findById(refundId)
                    ?: throw ValidationException("Refund request not found")

            // Verify user is seller or admin
            val user =
                UserDAO.findById(userId)
                    ?: throw ValidationException(Message.Errors.NOT_FOUND)

            val isAdmin = user.userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
            val isSeller = findSellerByUserId(userId) != null

            if (!isAdmin && !isSeller) {
                throw ValidationException(Message.Errors.FORBIDDEN)
            }

            val validStatuses = listOf("APPROVED", "REJECTED", "REFUNDED")
            if (request.status !in validStatuses) {
                throw ValidationException("Invalid status. Must be one of: ${validStatuses.joinToString(", ")}")
            }

            refundReq.status = request.status
            refundReq.resolvedAt = LocalDateTime.now(ZoneOffset.UTC)

            if (request.refundAmount != null) {
                refundReq.refundAmount = BigDecimal.valueOf(request.refundAmount)
            }
            if (request.refundMethod != null) {
                refundReq.refundMethod = request.refundMethod
            }

            refundReq.response()
        }

    override suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse =
        query {
            val refundReq =
                RefundRequestDAO.findById(refundId)
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
