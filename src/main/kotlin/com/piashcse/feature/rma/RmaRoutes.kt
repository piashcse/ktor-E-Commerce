@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.rma

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.getCurrentUserType
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.rmaRoutes() {
    val service: RmaService by inject()
    customerOnlyAuth {
        writeRateLimit {
            /**
             * Create a return or exchange request for a delivered order item
             */
            post {
                call.respondCreated(service.create(call.currentUserId, call.receive<RmaCreateRequest>()))
            }.describe {
                summary = "Create return/exchange request"
                description = "Delivered, paid items only; quantity-aware"
                requestBody { jsonBody<RmaCreateRequest>() }
                responses { created<RmaResponse>("RMA created") }
            }

            /**
             * Customer - mark an approved RMA as shipped back
             *
             */
            post("{id}/ship") {
                val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
                call.respondOk(service.ship(id, call.currentUserId, call.receive<RmaShipRequest>().trackingNumber))
            }.describe {
                summary = "Mark RMA as shipped"
                description = "Customer ships the approved return back"
                requestBody { jsonBody<RmaShipRequest>() }
                responses { ok<RmaResponse>("RMA in transit") }
            }

            /**
             * Customer - cancel a pending RMA
             *
             */
            post("{id}/cancel") {
                val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
                call.respondOk(service.cancel(id, call.currentUserId))
            }.describe {
                summary = "Cancel RMA"
                description = "Allowed from REQUESTED or APPROVED"
                responses { ok<RmaResponse>("RMA cancelled") }
            }
        }

        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(service.getUserRmas(call.currentUserId, limit, offset))
        }.describe {
            summary = "List my RMAs"
            description = "Newest first"
        }
    }
}

fun Route.rmaSellerRoutes() {
    val service: RmaService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.getSellerRmas(call.currentUserId, limit, offset))
    }.describe {
        summary = "Seller: list RMAs"
        description = "Scoped to the seller's shop"
    }
    sellerWriteRateLimit {
        put("{id}/status") {
            val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
            call.respondOk(
                service.updateStatus(
                    id,
                    call.receive<RmaStatusUpdateRequest>(),
                    call.currentUserId,
                    call.getCurrentUserType() ?: UserType.SELLER,
                ),
            )
        }.describe {
            summary = "Seller: update RMA status"
            description = "Approve, reject, receive or complete a return/exchange"
            requestBody { jsonBody<RmaStatusUpdateRequest>() }
            responses { ok<RmaResponse>("RMA updated") }
        }
    }
}

fun Route.rmaAdminRoutes() {
    val service: RmaService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.getAdminRmas(limit, offset, call.request.queryParameters["status"]))
    }.describe {
        summary = "Admin: list RMAs"
        description = "Optional status filter"
    }
    adminWriteRateLimit {
        put("{id}/status") {
            val id = call.parameters["id"] ?: throw UnauthorizedException(Message.Errors.MISSING_PARAMETER)
            call.respondOk(
                service.updateStatus(
                    id,
                    call.receive<RmaStatusUpdateRequest>(),
                    call.currentUserId,
                    UserType.ADMIN,
                ),
            )
        }.describe {
            summary = "Admin: update RMA status"
            description = "Approve, reject, receive or complete a return/exchange"
            requestBody { jsonBody<RmaStatusUpdateRequest>() }
            responses { ok<RmaResponse>("RMA updated") }
        }
    }
}
