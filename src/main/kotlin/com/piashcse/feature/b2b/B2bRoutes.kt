@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.b2b

import com.piashcse.constants.B2bAccountStatus
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Buyer B2B account. */
fun Route.b2bAccountRoutes() {
    val service: B2bService by inject()
    requireRole {
        get {
            call.respondOk(service.account(call.currentUserId))
        }.describe { summary = "Get my B2B account" }

        post {
            call.respondOk(service.apply(call.currentUserId, call.receive<B2bAccountRequest>()))
        }.describe {
            summary = "Apply for or update a B2B account"
            requestBody { jsonBody<B2bAccountRequest>() }
            responses {
                ok<B2bAccountResponse>("B2B account")
                standardErrors()
            }
        }
    }
}

/** Buyer quote lifecycle. */
fun Route.b2bQuoteRoutes() {
    val service: B2bService by inject()
    requireRole {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(service.myQuotes(call.currentUserId, limit, offset))
        }.describe { summary = "List my B2B quotes" }

        get("{id}") {
            call.respondOk(service.quote(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Get a B2B quote" }

        writeRateLimit {
            post {
                call.respondCreated(service.createQuote(call.currentUserId, call.receive<QuoteRequest>()))
            }.describe {
                summary = "Request a B2B quote"
                requestBody { jsonBody<QuoteRequest>() }
                responses {
                    created<QuoteResponse>("Quote requested")
                    standardErrors()
                }
            }

            post("{id}/accept") {
                call.respondCreated(service.accept(call.currentUserId, call.requirePathParameter("id")))
            }.describe {
                summary = "Accept a quote and create a purchase order"
                responses {
                    created<PurchaseOrderResponse>("Purchase order created")
                    standardErrors()
                }
            }
        }
    }
}

/** Buyer purchase orders. */
fun Route.b2bPurchaseOrderRoutes() {
    val service: B2bService by inject()
    requireRole {
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(service.myPurchaseOrders(call.currentUserId, limit, offset))
        }.describe { summary = "List my purchase orders" }

        get("{id}") {
            call.respondOk(service.purchaseOrder(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Get a purchase order" }
    }
}

/** Seller quote responses. */
fun Route.b2bSellerQuoteRoutes() {
    val service: B2bService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.sellerQuotes(call.currentUserId, limit, offset))
    }.describe { summary = "Seller: list B2B quote requests" }

    writeRateLimit {
        put("{id}") {
            call.respondOk(
                service.respond(
                    call.currentUserId,
                    call.requirePathParameter("id"),
                    call.receive<QuoteRespondRequest>(),
                ),
            )
        }.describe {
            summary = "Seller: price and send/reject a quote"
            requestBody { jsonBody<QuoteRespondRequest>() }
            responses {
                ok<QuoteResponse>("Quote updated")
                standardErrors()
            }
        }
    }
}

/** Seller purchase orders. */
fun Route.b2bSellerPurchaseOrderRoutes() {
    val service: B2bService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.sellerPurchaseOrders(call.currentUserId, limit, offset))
    }.describe { summary = "Seller: list purchase orders" }

    writeRateLimit {
        post("{id}/fulfill") {
            call.respondOk(service.fulfill(call.currentUserId, call.requirePathParameter("id")))
        }.describe { summary = "Seller: mark a purchase order fulfilled" }
    }
}

/** Admin B2B account approval/terms. */
fun Route.b2bAdminRoutes() {
    val service: B2bService by inject()
    get("accounts") {
        val (limit, offset) = call.paginateQueryParams()
        val status = call.request.queryParameters["status"]?.parseEnum<B2bAccountStatus>("B2B account status")
        call.respondOk(service.adminAccounts(status, limit, offset))
    }.describe { summary = "Admin: list B2B accounts" }

    get("purchase-orders") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.adminPurchaseOrders(limit, offset))
    }.describe { summary = "Admin: list B2B purchase orders" }

    adminWriteRateLimit {
        put("accounts/{userId}") {
            call.respondOk(
                service.updateAccount(
                    call.requirePathParameter("userId"),
                    call.currentUserId,
                    call.receive<B2bAccountUpdateRequest>(),
                ),
            )
        }.describe {
            summary = "Admin: approve/update a B2B account"
            requestBody { jsonBody<B2bAccountUpdateRequest>() }
            responses { ok<B2bAccountResponse>("B2B account updated") }
        }
    }
}
