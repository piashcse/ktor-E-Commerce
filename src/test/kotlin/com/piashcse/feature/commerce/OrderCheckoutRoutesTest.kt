package com.piashcse.feature.commerce

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.OrderStatus
import com.piashcse.feature.checkout.checkoutRoutes
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.shipping_address.ShippingAddressRepository
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.ShippingAddressResponse
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
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

class OrderRoutesTest {
    private val repo: OrderRepository = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<OrderRepository> { repo } })
            routing { route("/api/v1/orders") { orderRoutes() } }
        }
    }

    @Test
    fun `list orders without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/orders")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `list orders happy path returns 200`() =
        testApplication {
            coEvery { repo.getOrders(any(), any(), any()) } returns
                PaginatedResponse(listOf(sampleOrder()), PaginationMetadata(1, 20, 0))
            setup()
            val res =
                client.get("/api/v1/orders") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "o-1")
        }

    @Test
    fun `customer cannot confirm order returns 403 or 401`() =
        testApplication {
            setup()
            val res =
                client.patch("/api/v1/orders/status/o-1?status=CONFIRMED") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            // Route-level role gate rejects before repo is touched.
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `cancel order happy path returns 200`() =
        testApplication {
            coEvery { repo.cancelOrder(any(), any(), any(), any()) } returns sampleOrder()
            setup()
            val res =
                client.post("/api/v1/orders/o-1/cancel") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"reason":"changed mind"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `cancel order blank reason returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/orders/o-1/cancel") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"reason":""}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
}

class CheckoutRoutesTest {
    private val orderRepo: OrderRepository = mockk()
    private val addressRepo: ShippingAddressRepository = mockk()
    private val methodRepo: ShippingMethodRepository = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single<OrderRepository> { orderRepo }
                    single<ShippingAddressRepository> { addressRepo }
                    single<ShippingMethodRepository> { methodRepo }
                },
            )
            routing { route("/api/v1/checkout") { checkoutRoutes() } }
        }
    }

    @Test
    fun `checkout summary without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/checkout/summary") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"shippingAddressId":"a-1","shippingMethodId":"m-1"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `checkout summary happy path returns 200`() =
        testApplication {
            coEvery { orderRepo.getCheckoutSummary(any(), any()) } returns
                CheckoutSummaryResponse("100.00", "10.00", "5.00", "0.00", "115.00", 2)
            setup()
            val res =
                client.post("/api/v1/checkout/summary") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"shippingAddressId":"a-1","shippingMethodId":"m-1"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "115.00")
        }

    @Test
    fun `place order happy path returns 201`() =
        testApplication {
            coEvery { orderRepo.placeOrder(any(), any()) } returns listOf(sampleOrder())
            setup()
            val res =
                client.post("/api/v1/checkout/place-order") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"shippingAddressId":"a-1","shippingMethodId":"m-1"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `list shipping addresses happy path returns 200`() =
        testApplication {
            coEvery { addressRepo.getShippingAddresses(any()) } returns
                listOf(
                    ShippingAddressResponse(
                        "a-1", "user-1", "John", "Doe", "t@t.com", "123",
                        "1 Main St", "Dhaka", null, "BD", "1000", true,
                    ),
                )
            setup()
            val res =
                client.get("/api/v1/checkout/shipping-address") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `list shipping methods happy path returns 200`() =
        testApplication {
            coEvery { methodRepo.getShippingMethods() } returns
                listOf(ShippingMethodResponse("m-1", "Standard", null, "10.00", null))
            setup()
            val res =
                client.get("/api/v1/checkout/shipping-method") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}
