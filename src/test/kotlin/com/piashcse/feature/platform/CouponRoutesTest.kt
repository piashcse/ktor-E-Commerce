package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.CouponDiscountType
import com.piashcse.feature.coupon.CouponRepository
import com.piashcse.feature.coupon.couponAdminRoutes
import com.piashcse.feature.coupon.couponRoutes
import com.piashcse.model.response.CouponResponse
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

class CouponRoutesTest {
    private val repo: CouponRepository = mockk()

    private fun sample() =
        CouponResponse(
            id = "c-1",
            code = "SAVE10",
            discountType = CouponDiscountType.PERCENTAGE,
            discountValue = "10.00",
            minOrderAmount = "50.00",
            maxDiscountAmount = null,
            startDate = "2024-01-01",
            endDate = "2025-01-01",
            usageLimit = 100,
            usageCount = 0,
            isActive = true,
        )

    private fun validBody() =
        "{\"code\":\"SAVE10\",\"discountType\":\"PERCENTAGE\"," +
            "\"discountValue\":\"10.00\",\"minOrderAmount\":\"50.00\"," +
            "\"startDate\":\"2024-01-01\",\"endDate\":\"2025-01-01\"," +
            "\"usageLimit\":100,\"isActive\":true}"

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<CouponRepository> { repo } })
            routing {
                route("/api/v1/coupons") { couponRoutes() }
                route("/api/v1/admin/coupons") { adminAuth { couponAdminRoutes() } }
            }
        }
    }

    @Test
    fun `public get by code happy path returns 200`() =
        testApplication {
            coEvery { repo.getCouponByCode("SAVE10") } returns sample()
            setup()
            val res = client.get("/api/v1/coupons/SAVE10")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "SAVE10")
        }

    @Test
    fun `public get unknown code returns 404`() =
        testApplication {
            coEvery { repo.getCouponByCode("NOPE") } returns null
            setup()
            val res = client.get("/api/v1/coupons/NOPE")
            assertEquals(HttpStatusCode.NotFound, res.status)
        }

    @Test
    fun `admin create happy path returns 201`() =
        testApplication {
            coEvery { repo.createCoupon(any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/admin/coupons") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "SAVE10")
        }

    @Test
    fun `admin create without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/coupons") {
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin create with customer token returns 403`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/coupons") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin create invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/coupons") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin update happy path returns 200`() =
        testApplication {
            coEvery { repo.updateCoupon("c-1", any()) } returns sample()
            setup()
            val res =
                client.put("/api/v1/admin/coupons/c-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin update without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/admin/coupons/c-1") {
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin delete happy path returns 200`() =
        testApplication {
            coEvery { repo.deleteCoupon("c-1") } returns true
            setup()
            val res =
                client.delete("/api/v1/admin/coupons/c-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin delete without token returns 401`() =
        testApplication {
            setup()
            val res = client.delete("/api/v1/admin/coupons/c-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin list happy path returns 200`() =
        testApplication {
            coEvery { repo.getCoupons(any(), any()) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 10, 0))
            setup()
            val res =
                client.get("/api/v1/admin/coupons") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "SAVE10")
        }

    @Test
    fun `admin list without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/coupons")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
