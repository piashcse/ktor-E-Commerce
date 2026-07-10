package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public/User/Seller refund request routes.
 */
fun Route.refundRequestRoutes() {
    val refundRequestRepo: RefundRequestRepository by inject()
    customerAuth {
        /**
         * @tag Refund-Request
         * @description Create a refund request for an order item
         */
        post("{orderId}") {
            call.respondCreated(refundRequestRepo.createRefundRequest(call.currentUserId, call.requirePathParameter("orderId"), call.receive<RefundRequestRequest>()))
        }

        /**
         * @tag Refund-Request
         * @description Mark an approved refund as shipped
         */
        post("{id}/ship") {
            call.respondOk(refundRequestRepo.shipRefund(call.requirePathParameter("id"), call.receive<ShipRefundRequest>(), call.currentUserId))
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Refund-Request
         * @description Get refund requests for an order
         */
        get("order/{orderId}") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(refundRequestRepo.getRefundsByOrderId(call.requirePathParameter("orderId"), call.currentUserId, call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED), limit, offset))
        }

        /**
         * @tag Refund-Request
         * @description Get refund request details
         */
        get("{id}") {
            val refundRequest = refundRequestRepo.getRefundById(call.requirePathParameter("id"), call.currentUserId, call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED))
            if (refundRequest != null) {
                call.respondOk(refundRequest)
            } else {
                call.respond(HttpStatusCode.NotFound, "Refund request not found")
            }
        }
    }
}

/**
 * Seller refund management routes.
 */
fun Route.refundSellerRoutes() {
    val refundRequestRepo: RefundRequestRepository by inject()
    /**
     * @tag Refund
     * @description Seller: Update refund request status
     */
    put("{id}/status") {
        call.respondOk(refundRequestRepo.updateRefundStatus(call.requirePathParameter("id"), call.receive<UpdateRefundStatusRequest>(), call.currentUserId))
    }
}

/**
 * Admin refund management routes.
 */
fun Route.refundAdminRoutes() {
    val refundRequestRepo: RefundRequestRepository by inject()
    /**
     * @tag Refund
     * @description Admin: Get all refund requests for an order
     */
    get("order/{orderId}") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(refundRequestRepo.getRefundsByOrderId(call.requirePathParameter("orderId"), call.currentUserId, UserType.ADMIN, limit, offset))
    }

    /**
     * @tag Refund
     * @description Admin: Update refund status
     */
    put("{id}/status") {
        call.respondOk(refundRequestRepo.updateRefundStatus(call.requirePathParameter("id"), call.receive<UpdateRefundStatusRequest>(), call.currentUserId))
    }
}
