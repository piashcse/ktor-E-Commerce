package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.PromotionType
import com.piashcse.feature.promotion.PromotionResponse
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.promotion.promotionAdminRoutes
import com.piashcse.plugin.adminAuth
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

class PromotionRoutesTest {
    private val service: PromotionService = mockk()

    private fun sample() =
        PromotionResponse(
            id = "promo-1",
            name = "10% off",
            type = PromotionType.PERCENTAGE,
            value = "10.00",
            minOrderAmount = "0.00",
            maxDiscountAmount = null,
            startsAt = "2026-01-01T00:00:00Z",
            endsAt = "2026-12-31T00:00:00Z",
            isActive = true,
            priority = 1,
            usageLimit = null,
            usageCount = 0,
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/admin/promotions") { adminAuth { promotionAdminRoutes() } } }
        }
    }

    @Test
    fun `admin lists promotions`() =
        testApplication {
            coEvery { service.list(null, 20, 0) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 20, 0))
            setup()
            val res =
                client.get("/api/v1/admin/promotions") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "promo-1")
        }

    @Test
    fun `admin creates a promotion`() =
        testApplication {
            coEvery { service.create(any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/admin/promotions") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {"name":"10% off","type":"PERCENTAGE","value":10,
                         "startsAt":"2026-01-01T00:00:00","endsAt":"2026-12-31T00:00:00"}
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "promo-1")
        }

    @Test
    fun `invalid percentage is rejected`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/promotions") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {"name":"bad","type":"PERCENTAGE","value":150,
                         "startsAt":"2026-01-01T00:00:00","endsAt":"2026-12-31T00:00:00"}
                        """.trimIndent(),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `customer cannot manage promotions`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/promotions") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
