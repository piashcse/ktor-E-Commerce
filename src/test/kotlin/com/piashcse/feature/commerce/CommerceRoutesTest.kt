package com.piashcse.feature.commerce

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.database.entities.Cart
import com.piashcse.feature.cart.CartRepository
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.wishlist.WishListRepository
import com.piashcse.feature.wishlist.wishListRoutes
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.PaymentResponse
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

private fun sampleOrder() =
    OrderResponse(
        orderId = "o-1",
        orderNumber = "ORD-20240101-0001",
        subTotal = "100.00",
        total = "115.00",
        status = OrderStatus.PENDING,
    )

private fun samplePayment() =
    PaymentResponse(
        id = "pay-1",
        orderId = "o-1",
        amount = "115.00",
        status = PaymentStatus.COMPLETED,
        paymentMethod = PaymentMethod.COD,
        transactionId = "tx-1",
    )

private fun sampleRefund() =
    RefundRequestResponse(
        id = "r-1",
        orderItemId = "oi-1",
        orderId = "o-1",
        userId = "user-1",
        reason = "damaged",
        images = null,
        status = RefundStatus.PENDING,
        refundAmount = null,
        refundMethod = null,
        trackingNumber = null,
        requestedAt = "2024-01-01T00:00:00",
        resolvedAt = null,
        createdAt = "2024-01-01T00:00:00",
        updatedAt = "2024-01-01T00:00:00",
    )

class CartRoutesTest {
    private val repo: CartRepository = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(org.koin.dsl.module { single<CartRepository> { repo } })
            routing { route("/api/v1/carts") { cartRoutes() } }
        }
    }

    @Test
    fun `create cart without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/carts") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"productId":"p-1","quantity":2}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `create cart happy path returns 201`() =
        testApplication {
            coEvery { repo.createCart(any(), any(), any()) } returns
                com.piashcse.database.entities.Cart("p-1", 2, null)
            setup()
            val res =
                client.post("/api/v1/carts") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"productId":"p-1","quantity":2}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `get cart summary happy path returns 200`() =
        testApplication {
            coEvery { repo.getCartSummary(any()) } returns
                CartSummaryResponse(emptyList(), "0.00", "0.00", 0)
            setup()
            val res =
                client.get("/api/v1/carts/summary") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `clear cart happy path returns 200`() =
        testApplication {
            coEvery { repo.clearCart(any()) } returns true
            setup()
            val res =
                client.delete("/api/v1/carts/all") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}

class WishlistRoutesTest {
    private val repo: WishListRepository = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(org.koin.dsl.module { single<WishListRepository> { repo } })
            routing { route("/api/v1/wishlists") { wishListRoutes() } }
        }
    }

    @Test
    fun `wishlist without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/wishlists")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `check product happy path returns 200`() =
        testApplication {
            coEvery { repo.isProductInWishList(any(), any()) } returns true
            setup()
            val res =
                client.get("/api/v1/wishlists/check?productId=p-1") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "true")
        }

    @Test
    fun `get wishlist happy path returns 200`() =
        testApplication {
            coEvery { repo.getWishList(any(), any(), any()) } returns
                PaginatedResponse(emptyList(), PaginationMetadata(0, 20, 0))
            setup()
            val res =
                client.get("/api/v1/wishlists") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}

class PaymentRoutesTest {
    private val repo: PaymentRepository = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(org.koin.dsl.module { single<PaymentRepository> { repo } })
            routing { route("/api/v1/payments") { paymentRoutes() } }
        }
    }

    @Test
    fun `create payment without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/payments") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"orderId":"o-1","amount":"115.00","status":"COMPLETED","paymentMethod":"COD","transactionId":"tx-1"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `create payment happy path returns 201`() =
        testApplication {
            coEvery { repo.createPayment(any(), any()) } returns samplePayment()
            setup()
            val res =
                client.post("/api/v1/payments") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"orderId":"o-1","amount":"115.00","status":"COMPLETED","paymentMethod":"COD","transactionId":"tx-1"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "pay-1")
        }

    @Test
    fun `get payment by id happy path returns 200`() =
        testApplication {
            coEvery { repo.getPaymentById(any(), any()) } returns samplePayment()
            setup()
            val res =
                client.get("/api/v1/payments/pay-1") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}

class RefundRoutesTest {
    private val repo: RefundRequestRepository = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(org.koin.dsl.module { single<RefundRequestRepository> { repo } })
            routing { route("/api/v1/refund-requests") { refundRequestRoutes() } }
        }
    }

    @Test
    fun `create refund without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/refund-requests/o-1") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"orderItemId":"oi-1","reason":"damaged"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `create refund happy path returns 201`() =
        testApplication {
            coEvery { repo.createRefundRequest(any(), any(), any()) } returns sampleRefund()
            setup()
            val res =
                client.post("/api/v1/refund-requests/o-1") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"orderItemId":"oi-1","reason":"damaged"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "r-1")
        }

    @Test
    fun `ship refund happy path returns 200`() =
        testApplication {
            coEvery { repo.shipRefund(any(), any(), any()) } returns sampleRefund()
            setup()
            val res =
                client.post("/api/v1/refund-requests/r-1/ship") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"trackingNumber":"TRACK-1"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}
