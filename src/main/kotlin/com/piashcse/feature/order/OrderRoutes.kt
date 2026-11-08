@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.order
import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.response.OrderResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.UnauthorizedException
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.Instant

@Serializable
data class ShipOrderRequest(val trackingNumber: String)

/**
 * Order-related routes for customers and sellers.
 */
fun Route.orderRoutes() {
    val orderRepo: OrderRepository by inject()

    generalRateLimit {
        /**
         * Public guest order tracking by order number + checkout email
         */
        get("track") {
            val orderNumber = call.requireQueryParameter("orderNumber")
            val email = call.requireQueryParameter("email")
            val order =
                orderRepo.getOrderForGuest(orderNumber, email)
                    ?: throw NotFoundException(Message.Orders.NOT_FOUND)
            call.respondOk(order)
        }.describe {
            summary = "Guest order tracking"
            description = "Lookup by order number + checkout email"
            parameters {
                query("orderNumber") { required = true }
                query("email") { required = true }
            }
            responses { ok<OrderResponse>("Order") }
        }
    }

    customerOnlyAuth {
        generalRateLimit {
            /**
             * Retrieve all orders for the authenticated customer
             */
            get {
                val (limit, offset) = call.paginateQueryParams()
                call.respondOk(orderRepo.getOrders(call.currentUserId, limit, offset))
            }.describe { summary = "List my orders" }

            /**
             * Keyset-paginated order history (`cursor` + `limit`)
             */
            get("cursor") {
                val (limit, _) = call.paginateQueryParams()
                call.respondOk(
                    orderRepo.getOrdersCursor(
                        call.currentUserId,
                        limit,
                        call.request.queryParameters["cursor"],
                    ),
                )
            }.describe { summary = "Cursor-paginated order history" }
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER) {
        writeRateLimit {
            /**
             * Update order status (Customer - CANCELED/RECEIVED, Seller - CONFIRMED/DELIVERED)
             *
             */
            patch("status/{id}") {
                val id = call.requirePathParameter("id")
                val status = call.requireQueryParameter("status").parseEnum<OrderStatus>("status")
                val userType = call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)

                if ((
                        status in listOf(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED) &&
                            userType != UserType.SELLER
                    ) ||
                    (status in listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED) && userType != UserType.CUSTOMER)
                ) {
                    throw ForbiddenException(Message.Orders.STATUS_NOT_ALLOWED)
                }

                call.respondOk(orderRepo.updateOrderStatus(call.currentUserId, id, status))
            }.describe {
                summary = "Update order status"
                description = "Customers: CANCELED/RECEIVED; sellers: CONFIRMED/SHIPPED/DELIVERED"
                parameters { query("status") { required = true } }
                responses { ok<OrderResponse>("Updated order") }
            }

            /**
             * Seller - mark a paid order as shipped with a tracking number
             *
             */
            post("{id}/shipping") {
                val userType = call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED)
                if (userType != UserType.SELLER) {
                    throw ForbiddenException(Message.Orders.STATUS_NOT_ALLOWED)
                }
                val body = call.receive<ShipOrderRequest>()
                call.respondOk(orderRepo.shipOrder(call.requirePathParameter("id"), call.currentUserId, body.trackingNumber))
            }.describe {
                summary = "Seller: mark order shipped"
                requestBody { jsonBody<ShipOrderRequest>() }
                responses { ok<OrderResponse>("Shipped order") }
            }

            /**
             * Cancel an order
             */
            post("{id}/cancel") {
                call.respondOk(
                    orderRepo.cancelOrder(
                        call.requirePathParameter("id"),
                        call.currentUserId,
                        call.receive<CancelOrderRequest>().reason,
                        call.getCurrentUserType() ?: throw UnauthorizedException(Message.Errors.UNAUTHORIZED),
                    ),
                )
            }.describe {
                summary = "Cancel order"
                requestBody { jsonBody<CancelOrderRequest>() }
                responses { ok<OrderResponse>("Cancelled order") }
            }
        }
    }
}

/**
 * Seller order management routes.
 */
fun Route.orderSellerRoutes() {
    val orderRepo: OrderRepository by inject()
    /**
     * Seller - Retrieve orders for the seller's shop
     *
     */
    generalRateLimit {
        get {
            val (limit, offset) = call.paginateQueryParams()
            val status = call.request.queryParameters["status"]
            call.respondOk(orderRepo.getSellerOrders(call.currentUserId, limit, offset, status))
        }.describe {
            summary = "Seller: list orders"
            description = "Optional status filter"
        }
    }
}

/**
 * Admin order management routes.
 */
fun Route.orderAdminRoutes() {
    val orderRepo: OrderRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Update the status of any order
         *
         */
        patch("status/{id}") {
            val id = call.requirePathParameter("id")
            val status = call.requireQueryParameter("status").parseEnum<OrderStatus>("status")
            call.respondOk(orderRepo.updateOrderStatus(call.currentUserId, id, status))
        }

        /**
         * Admin - Mark a paid order as shipped
         *
         */
        post("{id}/shipping") {
            val body = call.receive<ShipOrderRequest>()
            call.respondOk(orderRepo.shipOrder(call.requirePathParameter("id"), call.currentUserId, body.trackingNumber))
        }

        /**
         * Admin - Cancel any order
         *
         */
        post("{id}/cancel") {
            call.respondOk(
                orderRepo.cancelOrder(
                    call.requirePathParameter("id"),
                    call.currentUserId,
                    call.receive<CancelOrderRequest>().reason,
                    UserType.ADMIN,
                ),
            )
        }
    }

    /**
     * Admin - Retrieve all orders with advanced filtering
     *
     */
    generalRateLimit {
        get {
            val (limit, offset) = call.paginateQueryParams()
            val status = call.request.queryParameters["status"]
            val startDate =
                call.request.queryParameters["startDate"]?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }
            val endDate =
                call.request.queryParameters["endDate"]?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }

            call.respondOk(orderRepo.getAdminOrders(limit, offset, status, startDate, endDate))
        }.describe {
            summary = "Admin: list orders"
            description = "Status and date-range filters"
        }
    }
}
