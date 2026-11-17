@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.rma

import com.piashcse.constants.RmaStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.BulkIdsRequest
import com.piashcse.model.request.BulkResultResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.queryInstant
import com.piashcse.utils.extension.requireUserType
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondCsv
import com.piashcse.utils.extension.respondOk
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.request.requireQueryParameter
import io.ktor.server.response.*
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
                val id = call.requirePathParameter("id")
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
                val id = call.requirePathParameter("id")
                call.respondOk(service.cancel(id, call.currentUserId))
            }.describe {
                summary = "Cancel RMA"
                description = "Allowed from REQUESTED or APPROVED"
                responses { ok<RmaResponse>("RMA cancelled") }
            }
        }

        get {
            val (limit, offset) = call.pageRequest()
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
        val (limit, offset) = call.pageRequest()
        val status = call.request.queryParameters["status"]?.parseEnum<RmaStatus>("RMA status")
        call.respondOk(
            service.getSellerRmas(
                call.currentUserId,
                status,
                call.request.queryParameters["q"],
                call.queryInstant("from"),
                call.queryInstant("to"),
                limit,
                offset,
            ),
        )
    }.describe {
        summary = "Seller: list RMAs"
        description = "Scoped to the seller's shop; optional status filter"
    }
    sellerWriteRateLimit {
        put("{id}/status") {
            val id = call.requirePathParameter("id")
            call.respondOk(
                service.updateStatus(
                    id,
                    call.receive<RmaStatusUpdateRequest>(),
                    call.currentUserId,
                    call.requireUserType(),
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
        val (limit, offset) = call.pageRequest()
        val status = call.request.queryParameters["status"]?.parseEnum<RmaStatus>("RMA status")
        call.respondOk(
            service.getAdminRmas(
                limit,
                offset,
                status,
                call.request.queryParameters["q"],
                call.queryInstant("from"),
                call.queryInstant("to"),
            ),
        )
    }.describe {
        summary = "Admin: list RMAs"
        description = "Optional status filter"
    }
    adminWriteRateLimit {
        put("{id}/status") {
            val id = call.requirePathParameter("id")
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

        get("export") {
            call.respondCsv(
                "rmas.csv",
                listOf("id", "order_number", "customer_email", "product", "status", "type", "quantity", "note", "created_at"),
                fetch = { limit, offset ->
                    service.getAdminRmas(
                        limit,
                        offset,
                        call.request.queryParameters["status"]?.parseEnum<RmaStatus>("RMA status"),
                        call.request.queryParameters["q"],
                        call.queryInstant("from"),
                        call.queryInstant("to"),
                    ).data
                },
                row = {
                    listOf(
                        it.id,
                        it.orderNumber.orEmpty(),
                        it.customerEmail.orEmpty(),
                        it.productName.orEmpty(),
                        it.status,
                        it.type,
                        it.quantity,
                        it.note.orEmpty(),
                        it.createdAt,
                    )
                },
            )
        }.describe { summary = "Admin: export RMAs CSV" }

        post("bulk/status") {
            val status = call.requireQueryParameter("status").parseEnum<RmaStatus>("RMA status")
            val body = call.receive<BulkIdsRequest>()
            val failed =
                body.ids.filter { id ->
                    runCatching {
                        service.updateStatus(
                            id,
                            RmaStatusUpdateRequest(status = status, note = body.reason),
                            call.currentUserId,
                            UserType.ADMIN,
                        )
                    }.isFailure
                }
            call.respondOk(BulkResultResponse.of(body.ids.size, failed))
        }.describe {
            summary = "Admin: bulk update RMA status"
            requestBody { jsonBody<BulkIdsRequest>() }
        }
    }
}
