package com.piashcse.feature.product

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.review_rating.ReviewRatingRepository
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.model.response.ReviewRatingResponse
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

class ReviewRoutesTest {
    private val reviewRepo: ReviewRatingRepository = mockk()

    private fun sampleReview() =
        ReviewRatingResponse(id = "rev-1", userId = "user-1", productId = "prod-1", reviewText = "Great product", rating = 5)

    private fun pageOfReviews() = PaginatedResponse(listOf(sampleReview()), PaginationMetadata(totalCount = 1, limit = 20, offset = 0))

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single<ReviewRatingRepository> { reviewRepo }
                },
            )
            routing {
                route("/api/v1/reviews") { reviewRatingRoutes() }
            }
        }
    }

    @Test
    fun `get reviews happy path returns 200`() =
        testApplication {
            coEvery { reviewRepo.getReviewRating(any(), any(), any()) } returns pageOfReviews()
            setup()
            val res = client.get("/api/v1/reviews?productId=prod-1")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Great product")
        }

    @Test
    fun `get reviews missing productId returns 400`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/reviews")
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `add review without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/reviews") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"productId":"prod-1","reviewText":"Great product","rating":5}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `add review happy path returns 201`() =
        testApplication {
            coEvery { reviewRepo.addReviewRating(any(), any()) } returns sampleReview()
            setup()
            val res =
                client.post("/api/v1/reviews") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"productId":"prod-1","reviewText":"Great product","rating":5}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Great product")
        }

    @Test
    fun `add review invalid rating returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/reviews") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"productId":"prod-1","reviewText":"Great product","rating":99}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `update review happy path returns 200`() =
        testApplication {
            coEvery { reviewRepo.updateReviewRating(any(), any(), any(), any()) } returns
                sampleReview().copy(reviewText = "Updated review", rating = 4)
            setup()
            val res =
                client.put("/api/v1/reviews/rev-1?review=Updated%20review&rating=4") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Updated review")
        }

    @Test
    fun `update review without token returns 401`() =
        testApplication {
            setup()
            val res = client.put("/api/v1/reviews/rev-1?review=Updated&rating=4")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `update review missing params returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/reviews/rev-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `update review malformed rating returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/reviews/rev-1?review=Updated&rating=abc") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `delete review happy path returns 200`() =
        testApplication {
            coEvery { reviewRepo.deleteReviewRating(any(), any()) } returns "Review deleted successfully"
            setup()
            val res =
                client.delete("/api/v1/reviews/rev-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "deleted")
        }

    @Test
    fun `delete review without token returns 401`() =
        testApplication {
            setup()
            val res = client.delete("/api/v1/reviews/rev-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `mark helpful happy path returns 200`() =
        testApplication {
            coEvery { reviewRepo.markHelpful(any(), any()) } returns sampleReview()
            setup()
            val res =
                client.post("/api/v1/reviews/rev-1/helpful?helpful=true") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Great product")
        }

    @Test
    fun `mark helpful without token returns 401`() =
        testApplication {
            setup()
            val res = client.post("/api/v1/reviews/rev-1/helpful")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
