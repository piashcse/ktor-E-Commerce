package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.DisputeType
import com.piashcse.constants.ReviewStatus
import com.piashcse.feature.seller.CommissionRuleResponse
import com.piashcse.feature.seller.CommissionService
import com.piashcse.feature.seller.DisputeResponse
import com.piashcse.feature.seller.EffectiveCommissionResponse
import com.piashcse.feature.seller.SellerReviewResponse
import com.piashcse.feature.seller.SellerToolsService
import com.piashcse.feature.seller.commissionAdminRoutes
import com.piashcse.feature.seller.commissionSellerRoutes
import com.piashcse.feature.seller.disputeAdminRoutes
import com.piashcse.feature.seller.disputeRoutes
import com.piashcse.feature.seller.sellerOwnReviewRoutes
import com.piashcse.feature.seller.sellerPublicRoutes
import com.piashcse.feature.seller.sellerReviewAdminRoutes
import com.piashcse.feature.seller.sellerReviewRoutes
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

class SellerToolsRoutesTest {
    private val tools: SellerToolsService = mockk()
    private val commission: CommissionService = mockk()

    private fun review() = SellerReviewResponse("rev-1", "seller-1", "shop-1", "user-1", "order-1", 5, "Great", ReviewStatus.ACTIVE, null)

    private fun dispute() =
        DisputeResponse(
            "disp-1",
            "order-1",
            "seller-1",
            "user-1",
            DisputeType.ORDER_ISSUE,
            "Late delivery",
            "Parcel arrived late",
            DisputeStatus.OPEN,
            null,
            null,
            null,
            null,
        )

    private fun rule() = CommissionRuleResponse("rule-1", "Seller rate", "seller-1", null, null, "3.00", 10, true)

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single { tools }
                    single { commission }
                },
            )
            routing {
                route("/api/v1/sellers") { sellerPublicRoutes() }
                route("/api/v1/seller-reviews") { sellerReviewRoutes() }
                route("/api/v1/seller/reviews") { sellerAuth { sellerOwnReviewRoutes() } }
                route("/api/v1/seller/commission") { sellerAuth { commissionSellerRoutes() } }
                route("/api/v1/disputes") { disputeRoutes() }
                route("/api/v1/admin/seller-reviews") { adminAuth { sellerReviewAdminRoutes() } }
                route("/api/v1/admin/disputes") { adminAuth { disputeAdminRoutes() } }
                route("/api/v1/admin/commission-rules") { adminAuth { commissionAdminRoutes() } }
            }
        }
    }

    @Test
    fun `public seller reviews are readable`() =
        testApplication {
            coEvery { tools.reviewsForSeller("seller-1", 20, 0) } returns
                com.piashcse.utils.common.PaginatedResponse(
                    listOf(review()),
                    com.piashcse.utils.common.PaginationMetadata(1, 20, 0),
                )
            setup()
            val res = client.get("/api/v1/sellers/seller-1/reviews")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "rev-1")
        }

    @Test
    fun `customer creates a seller review`() =
        testApplication {
            coEvery { tools.createReview("user-1", any()) } returns review()
            setup()
            val res =
                client.post("/api/v1/seller-reviews") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"orderId":"order-1","rating":5,"comment":"Great"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "rev-1")
        }

    @Test
    fun `seller reads own reviews`() =
        testApplication {
            coEvery { tools.reviewsForSellerUser("seller-user", 20, 0) } returns
                com.piashcse.utils.common.PaginatedResponse(
                    listOf(review()),
                    com.piashcse.utils.common.PaginationMetadata(1, 20, 0),
                )
            setup()
            val res =
                client.get("/api/v1/seller/reviews") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin moderates a review`() =
        testApplication {
            coEvery { tools.updateReviewStatus("rev-1", ReviewStatus.HIDDEN) } returns
                review().copy(status = ReviewStatus.HIDDEN)
            setup()
            val res =
                client.put("/api/v1/admin/seller-reviews/rev-1/status") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"HIDDEN"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `buyer opens a dispute`() =
        testApplication {
            coEvery { tools.createDispute("user-1", any()) } returns dispute()
            setup()
            val res =
                client.post("/api/v1/disputes") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"orderId":"order-1","type":"ORDER_ISSUE","subject":"Late delivery","description":"Parcel arrived late"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `admin resolves a dispute`() =
        testApplication {
            coEvery { tools.updateDispute("disp-1", "admin-1", any()) } returns
                dispute().copy(status = DisputeStatus.RESOLVED)
            setup()
            val res =
                client.put("/api/v1/admin/disputes/disp-1") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"RESOLVED","resolution":"Refunded shipping"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `seller sees effective commission`() =
        testApplication {
            coEvery { commission.effectiveRate("seller-user", null) } returns
                EffectiveCommissionResponse("3.00", "rule")
            setup()
            val res =
                client.get("/api/v1/seller/commission") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "3.00")
        }

    @Test
    fun `admin creates a commission rule`() =
        testApplication {
            coEvery { commission.create(any()) } returns rule()
            setup()
            val res =
                client.post("/api/v1/admin/commission-rules") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Seller rate","sellerId":"seller-1","rate":3.00,"priority":10}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "rule-1")
        }
}
