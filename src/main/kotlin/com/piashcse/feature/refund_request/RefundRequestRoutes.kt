package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginationParameters
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
         * @tag Refund
         * @description Create a refund request for an order item
         */
        post("{orderId}") {
            val orderId = call.parameters["orderId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Order ID is required")
            val userId = call.currentUserId
            val requestBody = call.receive<RefundRequestRequest>()
            requestBody.validation()

            call.respond(HttpStatusCode.Created, refundRequestService.createRefundRequest(userId, orderId, requestBody))
        }

        /**
         * @tag Refund
         * @description Mark an approved refund as shipped
         */
        post("{id}/ship") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Refund ID is required")
            val userId = call.currentUserId
            val requestBody = call.receive<ShipRefundRequest>()
            requestBody.validation()

            call.respond(HttpStatusCode.OK, refundRequestService.shipRefund(id, requestBody, userId))
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        /**
         * @tag Refund
         * @description Get refund requests for an order
         */
        get("order/{orderId}") {
            val orderId = call.parameters["orderId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Order ID is required")
            val userId = call.currentUserId
            val userType = call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)
            val (limit, offset) = call.paginationParameters()

            call.respond(HttpStatusCode.OK, refundRequestService.getRefundsByOrderId(orderId, userId, userType, limit, offset))
        }

        /**
         * @tag Refund
         * @description Get refund request details
         */
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Refund ID is required")
            val refundRequest = refundRequestService.getRefundById(id)
            if (refundRequest != null) {
                call.respond(HttpStatusCode.OK, refundRequest)
            } else {
                call.respond(HttpStatusCode.NotFound, "Refund request not found")
            }
        }

        requireRole(UserType.SELLER) {
            /**
             * @tag Refund
             * @description Seller: Update refund request status
             */
            put("{id}/status") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Refund ID is required")
                val userId = call.currentUserId
                val requestBody = call.receive<UpdateRefundStatusRequest>()
                requestBody.validation()

                call.respond(HttpStatusCode.OK, refundRequestService.updateRefundStatus(id, requestBody, userId))
            }
        }
    }
}

/**
 * Admin refund management routes.
 */
fun Route.refundAdminRoutes(refundRequestService: RefundRequestService) {
    adminAuth {
        /**
         * @tag Refund
         * @description Admin: Get all refund requests for an order
         */
        get("order/{orderId}") {
            val orderId = call.parameters["orderId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Order ID is required")
            val userId = call.currentUserId
            val userType = UserType.ADMIN
            val (limit, offset) = call.paginationParameters()

            call.respond(HttpStatusCode.OK, refundRequestService.getRefundsByOrderId(orderId, userId, userType, limit, offset))
        }

        /**
         * @tag Refund
         * @description Admin: Update refund status
         */
        put("{id}/status") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Refund ID is required")
            val userId = call.currentUserId
            val requestBody = call.receive<UpdateRefundStatusRequest>()
            requestBody.validation()

            call.respond(HttpStatusCode.OK, refundRequestService.updateRefundStatus(id, requestBody, userId))
        }
    }
}
