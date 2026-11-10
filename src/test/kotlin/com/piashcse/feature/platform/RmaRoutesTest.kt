package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.RmaResolution
import com.piashcse.constants.RmaStatus
import com.piashcse.constants.RmaType
import com.piashcse.feature.rma.RmaResponse
import com.piashcse.feature.rma.RmaService
import com.piashcse.feature.rma.rmaRoutes
import com.piashcse.feature.rma.rmaSellerRoutes
import com.piashcse.plugin.sellerAuth
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

class RmaRoutesTest {
    private val service: RmaService = mockk()

    private fun sample() =
        RmaResponse(
            id = "rma-1",
            orderId = "o-1",
            orderItemId = "oi-1",
            userId = "user-1",
            shopId = "shop-1",
            quantity = 1,
            type = RmaType.RETURN,
            status = RmaStatus.REQUESTED,
            reason = "damaged",
            resolution = RmaResolution.REFUND,
            trackingNumber = null,
            replacementOrderId = null,
            refundRequestId = null,
            createdAt = "2024-01-01T00:00:00Z",
            resolvedAt = null,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/rma-requests") { rmaRoutes() }
                route("/api/v1/seller/rma-requests") { sellerAuth { rmaSellerRoutes() } }
            }
        }
    }

    @Test
    fun `customer creates an rma`() =
        testApplication {
            coEvery { service.create("user-1", any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/rma-requests") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"orderId":"o-1","orderItemId":"oi-1","quantity":1,"type":"RETURN","reason":"damaged"}""",
                    )
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "rma-1")
        }

    @Test
    fun `customer lists own rmas`() =
        testApplication {
            coEvery { service.getUserRmas("user-1", 20, 0) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(totalCount = 1, limit = 20, offset = 0))
            setup()
            val res =
                client.get("/api/v1/rma-requests") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `customer cannot access seller rma list`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/seller/rma-requests") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `anonymous cannot create rma`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/rma-requests") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"orderId":"o-1","orderItemId":"oi-1","quantity":1,"type":"RETURN","reason":"damaged"}""",
                    )
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
