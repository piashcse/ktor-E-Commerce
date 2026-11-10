package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.ReviewStatus
import com.piashcse.feature.review_rating.ReviewRatingRepository
import com.piashcse.feature.review_rating.reviewAdminRoutes
import com.piashcse.model.response.ReviewRatingResponse
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

class ReviewAdminRoutesTest {
    private val repo: ReviewRatingRepository = mockk()

    private fun sample() =
        ReviewRatingResponse(
            id = "rev-1",
            userId = "user-1",
            productId = "prod-1",
            reviewText = "Needs moderation",
            rating = 3,
            status = ReviewStatus.FLAGGED,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<ReviewRatingRepository> { repo } })
            routing { route("/api/v1/admin/reviews") { adminAuth { reviewAdminRoutes() } } }
        }
    }

    @Test
    fun `admin lists reviews by status`() =
        testApplication {
            coEvery { repo.getReviews(ReviewStatus.FLAGGED, 20, 0) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 20, 0))
            setup()
            val res =
                client.get("/api/v1/admin/reviews?status=FLAGGED") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "rev-1")
        }

    @Test
    fun `admin can hide a review`() =
        testApplication {
            coEvery { repo.moderateReview("rev-1", ReviewStatus.HIDDEN) } returns
                sample().copy(status = ReviewStatus.HIDDEN)
            setup()
            val res =
                client.put("/api/v1/admin/reviews/rev-1/status") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"HIDDEN"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "HIDDEN")
        }

    @Test
    fun `customer cannot moderate reviews`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/admin/reviews/rev-1/status") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"HIDDEN"}""")
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/admin/reviews").status)
        }
}
