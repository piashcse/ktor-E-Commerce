package com.piashcse.feature.commerce

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.refundAdminRoutes
import com.piashcse.feature.refund_request.refundSellerRoutes
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.request.CartRequest
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import org.valiktor.ConstraintViolationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SellerAdminOrderRoutesTest {
    private val repo: OrderRepository = mockk()

    private fun sampleOrder() =
        OrderResponse(
            orderId = "o-1",
            orderNumber = "ORD-1",
            subTotal = "100.00",
            total = "115.00",
            status = OrderStatus.PENDING,
        )

    @Test
    fun `seller orders happy path returns 200`() =
        testApplication {
            coEvery { repo.getSellerOrders(any(), any(), any(), any()) } returns
                PaginatedResponse(listOf(sampleOrder()), PaginationMetadata(1, 20, 0))
            application {
                installTestInfra(module { single<OrderRepository> { repo } })
                routing { route("/api/v1/seller") { sellerAuth { route("orders") { orderSellerRoutes() } } } }
            }
            val res =
                client.get("/api/v1/seller/orders") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `seller orders customer token returns 403`() =
        testApplication {
            application {
                installTestInfra(module { single<OrderRepository> { repo } })
                routing { route("/api/v1/seller") { sellerAuth { route("orders") { orderSellerRoutes() } } } }
            }
            val res =
                client.get("/api/v1/seller/orders") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin orders happy path returns 200`() =
        testApplication {
            coEvery { repo.getAdminOrders(any(), any(), any(), any(), any()) } returns
                PaginatedResponse(listOf(sampleOrder()), PaginationMetadata(1, 20, 0))
            application {
                installTestInfra(module { single<OrderRepository> { repo } })
                routing { route("/api/v1/admin") { adminAuth { route("orders") { orderAdminRoutes() } } } }
            }
            val res =
                client.get("/api/v1/admin/orders") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin update status happy path returns 200`() =
        testApplication {
            coEvery { repo.updateOrderStatus(any(), any(), any()) } returns sampleOrder()
            application {
                installTestInfra(module { single<OrderRepository> { repo } })
                routing { route("/api/v1/admin") { adminAuth { route("orders") { orderAdminRoutes() } } } }
            }
            val res =
                client.patch("/api/v1/admin/orders/status/o-1?status=CONFIRMED") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}

class SellerAdminRefundRoutesTest {
    private val repo: RefundRequestRepository = mockk()

    @Test
    fun `seller update refund happy path returns 200`() =
        testApplication {
            coEvery { repo.updateRefundStatus(any(), any(), any()) } returns
                RefundRequestResponse(
                    "r-1", "oi-1", "o-1", "user-1", "damaged", null,
                    RefundStatus.APPROVED, null, null, null,
                    "2024-01-01T00:00:00", null, "2024-01-01T00:00:00", "2024-01-01T00:00:00",
                )
            application {
                installTestInfra(module { single<RefundRequestRepository> { repo } })
                routing { route("/api/v1/seller") { sellerAuth { route("refund-requests") { refundSellerRoutes() } } } }
            }
            val res =
                client.put("/api/v1/seller/refund-requests/r-1/status") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"APPROVED"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin update refund happy path returns 200`() =
        testApplication {
            coEvery { repo.updateRefundStatus(any(), any(), any()) } returns
                RefundRequestResponse(
                    "r-1", "oi-1", "o-1", "user-1", "damaged", null,
                    RefundStatus.APPROVED, null, null, null,
                    "2024-01-01T00:00:00", null, "2024-01-01T00:00:00", "2024-01-01T00:00:00",
                )
            application {
                installTestInfra(module { single<RefundRequestRepository> { repo } })
                routing { route("/api/v1/admin") { adminAuth { route("refund-requests") { refundAdminRoutes() } } } }
            }
            val res =
                client.put("/api/v1/admin/refund-requests/r-1/status") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"REJECTED"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}

class CommerceValidationTest {
    @Test
    fun `cart request rejects blank product`() {
        assertFailsWith<ConstraintViolationException> {
            CartRequest("", 1)
        }
    }

    @Test
    fun `cart request valid`() {
        CartRequest("p-1", 2)
    }

    @Test
    fun `checkout request rejects blank ids`() {
        assertFailsWith<ConstraintViolationException> {
            CheckoutRequest("", "")
        }
    }

    @Test
    fun `checkout request rejects bad currency`() {
        assertFailsWith<IllegalArgumentException> {
            CheckoutRequest("a-1", "m-1", currency = "XXX")
        }
    }

    @Test
    fun `payment request rejects zero amount`() {
        assertFailsWith<ConstraintViolationException> {
            PaymentRequest("o-1", BigDecimal.ZERO, PaymentStatus.PENDING, PaymentMethod.COD, null)
        }
    }

    @Test
    fun `cancel order rejects blank reason`() {
        assertFailsWith<ConstraintViolationException> {
            CancelOrderRequest("")
        }
    }

    @Test
    fun `refund request rejects blank reason`() {
        assertFailsWith<ConstraintViolationException> {
            RefundRequestRequest("oi-1", "")
        }
    }

    @Test
    fun `ship refund requires tracking`() {
        ShipRefundRequest("TRACK-1")
    }

    @Test
    fun `update refund status valid`() {
        UpdateRefundStatusRequest(RefundStatus.APPROVED)
    }
}
