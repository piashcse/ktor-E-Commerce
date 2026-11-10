@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.refund_request

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.customerAuth
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject
import java.io.File

/**
 * Public/User/Seller refund request routes.
 */
fun Route.refundRequestRoutes() {
    val refundRequestRepo: RefundRequestRepository by inject()
    customerAuth {
        writeRateLimit {
            /**
             * Create a refund request for an order item
             */
            post("{orderId}") {
                call.respondCreated(
                    refundRequestRepo.createRefundRequest(
                        call.currentUserId,
                        call.requirePathParameter("orderId"),
                        call.receive<RefundRequestRequest>(),
                    ),
                )
            }.describe { summary = "Create a refund request for an order item" }

            /**
             * Mark an approved refund as shipped
             */
            post("{id}/ship") {
                call.respondOk(
                    refundRequestRepo.shipRefund(call.requirePathParameter("id"), call.receive<ShipRefundRequest>(), call.currentUserId),
                )
            }.describe { summary = "Mark an approved refund as shipped" }
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN) {
        /**
         * Get private refund evidence for an authorized refund request
         */
        get("{id}/evidence/{fileName}") {
            val refundId = call.requirePathParameter("id")
            val fileName = call.requirePathParameter("fileName")
            if (fileName.contains('/') || fileName.contains('\\') || fileName.contains("..")) {
                throw ValidationException("Invalid file name")
            }
            val refund =
                refundRequestRepo.getRefundById(
                    refundId,
                    call.currentUserId,
                    call.requireUserType(),
                ) ?: throw NotFoundException(Message.Refunds.NOT_FOUND)
            // Touch the record so authorization above is the gate, then serve the file.
            check(refund.id == refundId)
            val root = File(DotEnvConfig.uploadDir, "refund-images").canonicalFile
            val file = File(root, fileName).canonicalFile
            if (!file.canonicalPath.startsWith(root.canonicalPath) || !file.isFile) {
                throw NotFoundException("Evidence file not found")
            }
            call.response.header(HttpHeaders.CacheControl, "private, no-store")
            call.respondFile(file)
        }.describe { summary = "Get private refund evidence for an authorized refund request" }

        /**
         * Get refund requests for an order
         */
        get("order/{orderId}") {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(
                refundRequestRepo.getRefundsByOrderId(
                    call.requirePathParameter("orderId"),
                    call.currentUserId,
                    call.requireUserType(),
                    limit,
                    offset,
                ),
            )
        }.describe { summary = "Get refund requests for an order" }

        /**
         * Get refund request details
         */
        get("{id}") {
            val refundRequest =
                refundRequestRepo.getRefundById(
                    call.requirePathParameter("id"),
                    call.currentUserId,
                    call.requireUserType(),
                )
            if (refundRequest != null) {
                call.respondOk(refundRequest)
            } else {
                throw NotFoundException(Message.Refunds.NOT_FOUND)
            }
        }.describe { summary = "Get refund request details" }
    }
}

/**
 * Seller refund management routes.
 */
fun Route.refundSellerRoutes() {
    val refundRequestRepo: RefundRequestRepository by inject()
    sellerWriteRateLimit {
        /**
         * Seller - Update refund request status
         *
         */
        put("{id}/status") {
            call.respondOk(
                refundRequestRepo.updateRefundStatus(
                    call.requirePathParameter("id"),
                    call.receive<UpdateRefundStatusRequest>(),
                    call.currentUserId,
                ),
            )
        }.describe { summary = "Seller - Update refund request status" }
    }
}

/**
 * Admin refund management routes.
 */
fun Route.refundAdminRoutes() {
    val refundRequestRepo: RefundRequestRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Update refund status
         *
         */
        put("{id}/status") {
            call.respondOk(
                refundRequestRepo.updateRefundStatus(
                    call.requirePathParameter("id"),
                    call.receive<UpdateRefundStatusRequest>(),
                    call.currentUserId,
                ),
            )
        }.describe { summary = "Admin - Update refund status" }
    }

    /**
     * Admin - Get all refund requests for an order
     *
     */
    get("order/{orderId}") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(
            refundRequestRepo.getRefundsByOrderId(call.requirePathParameter("orderId"), call.currentUserId, UserType.ADMIN, limit, offset),
        )
    }.describe { summary = "Admin - Get all refund requests for an order" }
}
