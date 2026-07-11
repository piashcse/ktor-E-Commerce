package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
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
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

class RefundRequestRepositoryImpl : RefundRequestRepository {
    override suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse =
        query {
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            if (order.userId.value != userId) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            val orderItem =
                OrderItemDAO.find {
                    (OrderItemTable.id eq request.orderItemId.entityID(OrderItemTable)) and
                        (OrderItemTable.orderId eq orderId.entityID(OrderTable))
                }.firstOrNull() ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)

            val existingRefund =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderItemId eq orderItem.id) and
                        (RefundRequestTable.status eq RefundStatus.PENDING)
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
        userId: String,
        userType: UserType,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        query {
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

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
                .andWhere { RefundRequestTable.orderId eq orderId.entityID(OrderTable) }
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
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

            refundRequest.toRefundRequestResponse()
        }

    private fun orderBelongsToUserShop(
        orderId: String,
        userId: String,
    ): Boolean {
        val order = OrderDAO.findById(orderId) ?: return false
        val shopId = order.shopId?.value ?: return false
        val seller = SellerDAO.find {
            (SellerTable.userId eq userId) and (SellerTable.shopId eq shopId.entityID(ShopTable))
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
                    ?: throw ValidationException(Message.Refunds.NOT_FOUND)

            val user =
                UserDAO.findById(userId)
                    ?: throw ValidationException(Message.Errors.NOT_FOUND)

            val isAdmin = user.userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
            val isSeller = findSellerByUserId(userId) != null

            if (!isAdmin && !isSeller) {
                throw ValidationException(Message.Errors.FORBIDDEN)
            }

            if (request.status !in listOf(RefundStatus.APPROVED, RefundStatus.REJECTED, RefundStatus.REFUNDED)) {
                throw ValidationException(Message.Refunds.INVALID_STATUS)
            }

            refundReq.status = request.status
            refundReq.resolvedAt = LocalDateTime.now(ZoneOffset.UTC)

            if (request.refundAmount != null) {
                refundReq.refundAmount = BigDecimal.valueOf(request.refundAmount)
            }
            if (request.refundMethod != null) {
                refundReq.refundMethod = request.refundMethod
            }

            refundReq.toRefundRequestResponse()
        }

    override suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse =
        query {
            val refundReq =
                RefundRequestDAO.findById(refundId)
                    ?: throw ValidationException(Message.Refunds.NOT_FOUND)

            if (refundReq.userId.value != userId) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            if (refundReq.status != RefundStatus.APPROVED) {
                throw ValidationException(Message.Refunds.MUST_BE_APPROVED)
            }

            refundReq.trackingNumber = request.trackingNumber
            refundReq.status = RefundStatus.SHIPPED

            refundReq.toRefundRequestResponse()
        }
}
