package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.payout.PayoutRepository
import com.piashcse.feature.payout.PayoutResponse
import com.piashcse.feature.payout.payoutAdminRoutes
import com.piashcse.feature.payout.payoutSellerRoutes
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

class PayoutRoutesTest {
    private val repo: PayoutRepository = mockk()

    private fun sample() =
        PayoutResponse(
            id = "po-1",
            sellerId = "seller-1",
            orderId = "o-1",
            subTotal = "100.00",
            commissionAmount = "10.00",
            payoutAmount = "90.00",
            status = "PENDING",
            createdAt = "2024-01-01T00:00:00",
        )

    private fun page() = PaginatedResponse(listOf(sample()), PaginationMetadata(1, 10, 0))

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<PayoutRepository> { repo } })
            routing {
                route("/api/v1/seller/payouts") { payoutSellerRoutes() }
                route("/api/v1/admin/payouts") { payoutAdminRoutes() }
            }
        }
    }

    @Test
    fun `seller payouts happy path returns 200`() =
        testApplication {
            coEvery { repo.sellerPayouts(any(), any(), any()) } returns page()
            setup()
            val res =
                client.get("/api/v1/seller/payouts") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "po-1")
        }

    @Test
    fun `seller payouts without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/seller/payouts")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin payouts happy path returns 200`() =
        testApplication {
            coEvery { repo.allPayouts(any(), any(), any()) } returns page()
            setup()
            val res =
                client.get("/api/v1/admin/payouts") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "po-1")
        }

    @Test
    fun `admin payouts without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/payouts")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin payouts with seller token returns 403`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/payouts") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin mark-paid happy path returns 200`() =
        testApplication {
            coEvery { repo.markPaid("po-1") } returns sample().copy(status = "PAID")
            setup()
            val res =
                client.post("/api/v1/admin/payouts/po-1/pay") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "PAID")
        }

    @Test
    fun `admin mark-paid without token returns 401`() =
        testApplication {
            setup()
            val res = client.post("/api/v1/admin/payouts/po-1/pay")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin export happy path returns csv 200`() =
        testApplication {
            coEvery { repo.allPayouts(any(), any(), any()) } returns page()
            setup()
            val res =
                client.get("/api/v1/admin/payouts/export") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "id,seller_id")
            assertContains(res.bodyAsText(), "po-1")
        }

    @Test
    fun `admin export without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/payouts/export")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
