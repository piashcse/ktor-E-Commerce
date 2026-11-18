@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.order
import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.feature.invoice.InvoiceService
import com.piashcse.mapper.toPublicTrackingResponse
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.response.OrderResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.NotFoundException
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class ShipOrderRequest(val trackingNumber: String)

@Serializable
data class OrderMilestone(
    val key: String,
    val label: String,
    val occurredAt: String? = null,
)

@Serializable
data class OrderTimelineResponse(
    val orderNumber: String,
    val status: OrderStatus,
    val trackingNumber: String? = null,
    val milestones: List<OrderMilestone>,
)

private fun ApplicationCall.guestOrderCredentials(): Pair<String, String> =
    requireQueryParameter("orderNumber") to requireQueryParameter("email")

private fun OrderResponse.toTimeline(): OrderTimelineResponse =
    OrderTimelineResponse(
        orderNumber = orderNumber,
        status = status,
        trackingNumber = trackingNumber,
        milestones =
            listOf(
                OrderMilestone("PLACED", "Order placed", createdAt?.toString()),
                OrderMilestone("SHIPPED", "Shipped", shippingDate?.toString()),
                OrderMilestone("DELIVERED", "Delivered", deliveredDate?.toString()),
                OrderMilestone("COMPLETED", "Completed", completedDate?.toString()),
                OrderMilestone("CANCELED", "Canceled", canceledDate?.toString()),
            ),
    )

/**
 * Order-related routes for customers and sellers.
 */
fun Route.orderRoutes() {
    val orderRepo: OrderRepository by inject()

    guestOrderTrackingRoutes()

    customerOnlyAuth {
        generalRateLimit {
            /**
             * Retrieve all orders for the authenticated customer
             */
            get {
                val (limit, offset) = call.pageRequest()
                call.respondOk(
                    orderRepo.getOrders(
                        call.currentUserId,
                        limit,
                        offset,
                        call.request.queryParameters["status"],
                        call.request.queryParameters["q"],
                        call.queryInstant("from"),
                        call.queryInstant("to"),
                    ),
                )
            }.describe { summary = "List my orders" }

            /**
             * Keyset-paginated order history (`cursor` + `limit`)
             */
            get("cursor") {
                val cursor = call.cursorRequest()
                call.respondOk(
                    orderRepo.getOrdersCursor(
                        call.currentUserId,
                        cursor.limit,
                        cursor.cursor,
                    ),
                )
            }.describe { summary = "Cursor-paginated order history" }
        }
    }

    requireRole(UserType.CUSTOMER, UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN) {
        generalRateLimit {
            /**
             * Retrieve a single order by ID (customer owner, shop seller, or admin)
             */
            get("{id}") {
                val id = call.requirePathParameter("id")
                call.respondOk(orderRepo.getOrderById(id, call.currentUserId, call.requireUserType()))
            }.describe {
                summary = "Get order by ID"
                responses {
                    ok<OrderResponse>("Order")
                    standardErrors()
                }
            }
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
                OrderStatusPolicy.assertCanSet(call.requireUserType(), status)

                call.respondOk(orderRepo.updateOrderStatus(call.currentUserId, id, status))
            }.describe {
                summary = "Update order status"
                description = "Customers: CANCELED/RECEIVED; sellers: CONFIRMED/SHIPPED/DELIVERED"
                parameters { query("status") { required = true } }
                responses {
                    ok<OrderResponse>("Updated order")
                    standardErrors()
                }
            }

            /**
             * Seller - mark a paid order as shipped with a tracking number
             *
             */
            post("{id}/shipping") {
                OrderStatusPolicy.assertSeller(call.requireUserType())
                val body = call.receive<ShipOrderRequest>()
                call.respondOk(orderRepo.shipOrder(call.requirePathParameter("id"), call.currentUserId, body.trackingNumber))
            }.describe {
                summary = "Seller: mark order shipped"
                requestBody { jsonBody<ShipOrderRequest>() }
                responses {
                    ok<OrderResponse>("Shipped order")
                    standardErrors()
                }
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
                        call.requireUserType(),
                    ),
                )
            }.describe {
                summary = "Cancel order"
                requestBody { jsonBody<CancelOrderRequest>() }
                responses {
                    ok<OrderResponse>("Cancelled order")
                    standardErrors()
                }
            }
        }
    }
}

/** Public guest order tracking, timeline and printable invoice. */
private fun Route.guestOrderTrackingRoutes() {
    val orderRepo: OrderRepository by inject()
    val invoiceService: InvoiceService by inject()
    generalRateLimit {
        get("track") {
            val (orderNumber, email) = call.guestOrderCredentials()
            val order =
                orderRepo.getOrderForGuest(orderNumber, email)
                    ?: throw NotFoundException(Message.Orders.NOT_FOUND)
            call.respondOk(order.toPublicTrackingResponse())
        }.describe {
            summary = "Guest order tracking"
            description = "Lookup by order number + checkout email; returns a privacy-safe view"
            parameters {
                query("orderNumber") { required = true }
                query("email") { required = true }
            }
            responses {
                ok<com.piashcse.model.response.PublicOrderTrackingResponse>("Order tracking")
                standardErrors()
            }
        }

        get("track/timeline") {
            val (orderNumber, email) = call.guestOrderCredentials()
            val order =
                orderRepo.getOrderForGuest(orderNumber, email)
                    ?: throw NotFoundException(Message.Orders.NOT_FOUND)
            call.respondOk(order.toTimeline())
        }.describe {
            summary = "Guest order timeline"
            description = "Milestone dates for the tracking page"
            parameters {
                query("orderNumber") { required = true }
                query("email") { required = true }
            }
        }

        get("track/invoice") {
            val (orderNumber, email) = call.guestOrderCredentials()
            val html = invoiceService.invoiceForGuest(orderNumber, email)
            call.respondText(html, ContentType.Text.Html)
        }.describe {
            summary = "Guest order invoice"
            description = "Printable HTML invoice without requiring an account"
            parameters {
                query("orderNumber") { required = true }
                query("email") { required = true }
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
            val (limit, offset) = call.pageRequest()
            val status = call.request.queryParameters["status"]
            call.respondOk(
                orderRepo.getSellerOrders(
                    call.currentUserId,
                    limit,
                    offset,
                    status,
                    call.request.queryParameters["q"],
                    call.queryInstant("from"),
                    call.queryInstant("to"),
                ),
            )
        }.describe {
            summary = "Seller: list orders"
            description = "Optional status, q (order number/email) and from/to filters"
        }
        get("export") {
            call.respondCsv(
                "orders.csv",
                listOf("order_number", "status", "payment_status", "currency", "total", "contact_email", "created_at"),
                fetch = { limit, offset ->
                    orderRepo.getSellerOrders(
                        call.currentUserId,
                        limit,
                        offset,
                        call.request.queryParameters["status"],
                        call.request.queryParameters["q"],
                        call.queryInstant("from"),
                        call.queryInstant("to"),
                    ).data
                },
                row = {
                    listOf(
                        it.orderNumber,
                        it.status,
                        it.paymentStatus,
                        it.currency,
                        it.total,
                        it.contactEmail.orEmpty(),
                        it.createdAt,
                    )
                },
            )
        }.describe { summary = "Seller: export orders CSV" }

        get("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(orderRepo.getOrderById(id, call.currentUserId, call.requireUserType()))
        }.describe {
            summary = "Seller: get order by ID"
        }
    }

    writeRateLimit {
        post("{id}/shipping") {
            OrderStatusPolicy.assertSeller(call.requireUserType())
            val body = call.receive<ShipOrderRequest>()
            call.respondOk(orderRepo.shipOrder(call.requirePathParameter("id"), call.currentUserId, body.trackingNumber))
        }.describe {
            summary = "Seller: mark order shipped"
            requestBody { jsonBody<ShipOrderRequest>() }
        }
        post("{id}/cancel") {
            OrderStatusPolicy.assertSeller(call.requireUserType())
            call.respondOk(
                orderRepo.cancelOrder(
                    call.requirePathParameter("id"),
                    call.currentUserId,
                    call.receive<CancelOrderRequest>().reason,
                    call.requireUserType(),
                ),
            )
        }.describe {
            summary = "Seller: cancel order"
            requestBody { jsonBody<CancelOrderRequest>() }
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
        }.describe { summary = "Admin - Update the status of any order" }

        /**
         * Admin - Mark a paid order as shipped
         *
         */
        post("{id}/shipping") {
            val body = call.receive<ShipOrderRequest>()
            call.respondOk(orderRepo.shipOrder(call.requirePathParameter("id"), call.currentUserId, body.trackingNumber))
        }.describe { summary = "Admin - Mark a paid order as shipped" }

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
        }.describe { summary = "Admin - Cancel any order" }
    }

    /**
     * Admin - Retrieve all orders with advanced filtering
     *
     */
    generalRateLimit {
        get {
            val (limit, offset) = call.pageRequest()
            val status = call.request.queryParameters["status"]
            val startDate = call.queryInstant("startDate")
            val endDate = call.queryInstant("endDate")

            call.respondOk(
                orderRepo.getAdminOrders(
                    limit,
                    offset,
                    status,
                    startDate,
                    endDate,
                    call.request.queryParameters["q"],
                ),
            )
        }.describe {
            summary = "Admin: list orders"
            description = "Status, date-range and order-number/email filters"
        }

        get("export") {
            call.respondCsv(
                "orders.csv",
                listOf("order_number", "status", "payment_status", "currency", "total", "contact_email", "created_at"),
                fetch = { limit, offset ->
                    orderRepo.getAdminOrders(
                        limit,
                        offset,
                        call.request.queryParameters["status"],
                        call.queryInstant("startDate"),
                        call.queryInstant("endDate"),
                        call.request.queryParameters["q"],
                    ).data
                },
                row = {
                    listOf(
                        it.orderNumber,
                        it.status,
                        it.paymentStatus,
                        it.currency,
                        it.total,
                        it.contactEmail.orEmpty(),
                        it.createdAt,
                    )
                },
            )
        }.describe { summary = "Admin: export orders CSV" }

        get("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(orderRepo.getOrderById(id, call.currentUserId, call.requireUserType()))
        }.describe {
            summary = "Admin: get order by ID"
        }
    }
}
