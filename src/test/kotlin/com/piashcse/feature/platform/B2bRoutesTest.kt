package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.B2bAccountStatus
import com.piashcse.constants.B2bQuoteStatus
import com.piashcse.constants.PurchaseOrderStatus
import com.piashcse.feature.b2b.B2bAccountResponse
import com.piashcse.feature.b2b.B2bService
import com.piashcse.feature.b2b.PurchaseOrderResponse
import com.piashcse.feature.b2b.QuoteResponse
import com.piashcse.feature.b2b.b2bAccountRoutes
import com.piashcse.feature.b2b.b2bAdminRoutes
import com.piashcse.feature.b2b.b2bPurchaseOrderRoutes
import com.piashcse.feature.b2b.b2bQuoteRoutes
import com.piashcse.feature.b2b.b2bSellerPurchaseOrderRoutes
import com.piashcse.feature.b2b.b2bSellerQuoteRoutes
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
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

class B2bRoutesTest {
    private val service: B2bService = mockk()

    private fun account() =
        B2bAccountResponse(
            "acc-1", "user-1", "Acme Ltd", "TAX-1", "Alice", "123", "1000.00", 30,
            B2bAccountStatus.APPROVED, null, null,
        )

    private fun quote() =
        QuoteResponse(
            "quote-1", "user-1", "seller-1", "shop-1", B2bQuoteStatus.SENT, "USD",
            "100.00", "0.00", "100.00", null, null, null, emptyList(),
        )

    private fun po() =
        PurchaseOrderResponse(
            "po-1", "quote-1", null, "user-1", "seller-1", "shop-1", "PO-1", PurchaseOrderStatus.OPEN,
            "USD", "100.00", 30, null, null, null, null,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/b2b/account") { b2bAccountRoutes() }
                route("/api/v1/b2b/quotes") { b2bQuoteRoutes() }
                route("/api/v1/b2b/purchase-orders") { b2bPurchaseOrderRoutes() }
                route("/api/v1/seller/b2b/quotes") { sellerAuth { b2bSellerQuoteRoutes() } }
                route("/api/v1/seller/b2b/purchase-orders") { sellerAuth { b2bSellerPurchaseOrderRoutes() } }
                route("/api/v1/admin/b2b") { adminAuth { b2bAdminRoutes() } }
            }
        }
    }

    @Test
    fun `buyer reads account`() =
        testApplication {
            coEvery { service.account("user-1") } returns account()
            setup()
            val res =
                client.get("/api/v1/b2b/account") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Acme Ltd")
        }

    @Test
    fun `buyer requests a quote`() =
        testApplication {
            coEvery { service.createQuote("user-1", any()) } returns quote().copy(status = B2bQuoteStatus.REQUESTED)
            setup()
            val res =
                client.post("/api/v1/b2b/quotes") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"items":[{"productId":"p-1","quantity":10}],"validDays":14}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "quote-1")
        }

    @Test
    fun `seller sends a quote`() =
        testApplication {
            coEvery { service.respond("seller-user", "quote-1", any()) } returns quote()
            setup()
            val res =
                client.put("/api/v1/seller/b2b/quotes/quote-1") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"SENT"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `buyer accepts a quote`() =
        testApplication {
            coEvery { service.accept("user-1", "quote-1") } returns po()
            setup()
            val res =
                client.post("/api/v1/b2b/quotes/quote-1/accept") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "PO-1")
        }

    @Test
    fun `seller fulfills a purchase order`() =
        testApplication {
            coEvery { service.fulfill("seller-user", "po-1") } returns po().copy(status = PurchaseOrderStatus.FULFILLED)
            setup()
            val res =
                client.post("/api/v1/seller/b2b/purchase-orders/po-1/fulfill") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "FULFILLED")
        }

    @Test
    fun `admin approves an account`() =
        testApplication {
            coEvery { service.updateAccount("user-1", "admin-1", any()) } returns account()
            setup()
            val res =
                client.put("/api/v1/admin/b2b/accounts/user-1") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"APPROVED","creditLimit":1000.00,"paymentTermsDays":30}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `customers cannot access admin b2b routes`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/b2b/accounts") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
