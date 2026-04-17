package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.UnauthorizedException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.refundRequestRoutes(refundRequestService: RefundRequestService) {
    route("/refund-requests") {

        // Create refund request - customer only
        authenticate(RoleManagement.CUSTOMER.role) {
            /**
             * @tag Refund
             * @description Create a refund request for an order item
             * @operationId createRefundRequest
             * @path orderId Unique identifier of the order
             * @body RefundRequestRequest Refund request details
             * @response 200 Refund request created successfully
             * @security jwtToken
             */
            post("{orderId}") {
                val orderId = call.parameters["orderId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Order ID is required")

                val userId = call.currentUserId
                val requestBody = call.receive<RefundRequestRequest>()
                requestBody.validation()

                call.respond(
                    HttpStatusCode.Created,
                    refundRequestService.createRefundRequest(userId, orderId, requestBody)
                )
            }

            /**
             * @tag Refund
             * @description Mark an approved refund as shipped with tracking number
             * @operationId shipRefund
             * @path id Unique identifier of the refund request
             * @body ShipRefundRequest Tracking number
             * @response 200 Refund marked as shipped
             * @security jwtToken
             */
            post("{id}/ship") {
                val id = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Refund ID is required")

                val userId = call.currentUserId
                val requestBody = call.receive<ShipRefundRequest>()
                requestBody.validation()

                call.respond(
                    HttpStatusCode.OK,
                    refundRequestService.shipRefund(id, requestBody, userId)
                )
            }
        }

        // Get refunds by order - customer/seller/admin
        authenticate(
            RoleManagement.CUSTOMER.role,
            RoleManagement.SELLER.role,
            RoleManagement.ADMIN.role,
            RoleManagement.SUPER_ADMIN.role
        ) {
            /**
             * @tag Refund
             * @description Get all refund requests for an order with pagination
             * @operationId getRefundsByOrderId
             * @path orderId (required) Unique identifier of the order
             * @query limit Maximum number of refund requests to return (default 20)
             * @query offset Number of refund requests to skip (default 0)
             * @response 200 Refund requests retrieved successfully
             * @security jwtToken
             */
            get("order/{orderId}") {
                val orderId = call.parameters["orderId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Order ID is required")

                val userId = call.currentUserId
                val userType = call.getCurrentUserType()
                    ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)

                val (limit, offset) = call.paginationParameters()

                call.respond(
                    HttpStatusCode.OK,
                    refundRequestService.getRefundsByOrderId(orderId, userId, userType, limit, offset)
                )
            }

            /**
             * @tag Refund
             * @description Get refund request details by ID
             * @operationId getRefundById
             * @path id Unique identifier of the refund request
             * @response 200 Refund request details retrieved successfully
             * @security jwtToken
             */
            get("{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Refund ID is required")

                val refundRequest = refundRequestService.getRefundById(id)
                if (refundRequest != null) {
                    call.respond(HttpStatusCode.OK, refundRequest)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Refund request not found")
                }
            }

            // Update refund status - seller/admin only
            authenticate(RoleManagement.SELLER.role, RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
                /**
                 * @tag Refund
                 * @description Update refund request status (approve/reject/refund)
                 * @operationId updateRefundStatus
                 * @path id Unique identifier of the refund request
                 * @body UpdateRefundStatusRequest New status and optional refund details
                 * @response 200 Refund status updated successfully
                 * @security jwtToken
                 */
                put("{id}/status") {
                    val id = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "Refund ID is required")

                    val userId = call.currentUserId
                    val requestBody = call.receive<UpdateRefundStatusRequest>()
                    requestBody.validation()

                    call.respond(
                        HttpStatusCode.OK,
                        refundRequestService.updateRefundStatus(id, requestBody, userId)
                    )
                }
            }
        }
    }
}
