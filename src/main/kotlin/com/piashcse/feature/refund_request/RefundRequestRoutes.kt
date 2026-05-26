package com.piashcse.feature.refund_request
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public/User/Seller refund request routes.
 */
fun Route.refundRequestRoutes(refundRequestService: RefundRequestService) {
    customerAuth {
        /**
         * @tag Refund-Request
         * @description Create a refund request for an order item
         */
        post("{orderId}") {
            val orderId = call.requirePathParameter("orderId")
            val userId = call.currentUserId
            val requestBody = call.receive<RefundRequestRequest>()

            call.respond(HttpStatusCode.Created, refundRequestService.createRefundRequest(userId, orderId, requestBody))
        }

        /**
         * @tag Refund-Request
         * @description Mark an approved refund as shipped
         */
        post("{id}/ship") {
            val id = call.requirePathParameter("id")
            val userId = call.currentUserId
            val requestBody = call.receive<ShipRefundRequest>()

            call.respond(HttpStatusCode.OK, refundRequestService.shipRefund(id, requestBody, userId))
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Refund-Request
         * @description Get refund requests for an order
         */
        get("order/{orderId}") {
            val orderId = call.requirePathParameter("orderId")
            val userId = call.currentUserId
            val userType = call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)
            val (limit, offset) = call.paginateQueryParams()

            call.respond(HttpStatusCode.OK, refundRequestService.getRefundsByOrderId(orderId, userId, userType, limit, offset))
        }

        /**
         * @tag Refund-Request
         * @description Get refund request details
         */
        get("{id}") {
            val id = call.requirePathParameter("id")
            val refundRequest = refundRequestService.getRefundById(id)
            if (refundRequest != null) {
                call.respond(HttpStatusCode.OK, refundRequest)
            } else {
                call.respond(HttpStatusCode.NotFound, "Refund request not found")
            }
        }
    }
}

/**
 * Seller refund management routes.
 */
fun Route.refundSellerRoutes(refundRequestService: RefundRequestService) {
    /**
     * @tag Refund
     * @description Seller: Update refund request status
     */
    put("{id}/status") {
        val id = call.requirePathParameter("id")
        val userId = call.currentUserId
        val requestBody = call.receive<UpdateRefundStatusRequest>()

        call.respond(HttpStatusCode.OK, refundRequestService.updateRefundStatus(id, requestBody, userId))
    }
}

/**
 * Admin refund management routes.
 */
fun Route.refundAdminRoutes(refundRequestService: RefundRequestService) {
    /**
     * @tag Refund
     * @description Admin: Get all refund requests for an order
     */
    get("order/{orderId}") {
        val orderId = call.requirePathParameter("orderId")
        val userId = call.currentUserId
        val userType = UserType.ADMIN
        val (limit, offset) = call.paginateQueryParams()

        call.respond(HttpStatusCode.OK, refundRequestService.getRefundsByOrderId(orderId, userId, userType, limit, offset))
    }

    /**
     * @tag Refund
     * @description Admin: Update refund status
     */
    put("{id}/status") {
        val id = call.requirePathParameter("id")
        val userId = call.currentUserId
        val requestBody = call.receive<UpdateRefundStatusRequest>()

        call.respond(HttpStatusCode.OK, refundRequestService.updateRefundStatus(id, requestBody, userId))
    }
}
