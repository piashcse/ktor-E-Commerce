package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.recommendation.RecommendationResponse
import com.piashcse.feature.recommendation.RecommendationService
import com.piashcse.feature.recommendation.recommendationRoutes
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class RecommendationRoutesTest {
    private val service: RecommendationService = mockk()

    private fun sample() = RecommendationResponse(strategy = "personalized", sourceProductId = null, items = emptyList())

    private fun io.ktor.server.testing.ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/recommendations") { recommendationRoutes() } }
        }
    }

    @Test
    fun `customer receives personalized recommendations`() =
        testApplication {
            coEvery { service.personalized("user-1", 10) } returns sample()
            setup()
            val res =
                client.get("/api/v1/recommendations") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "personalized")
        }

    @Test
    fun `limit is clamped to the safe maximum`() =
        testApplication {
            coEvery { service.personalized("user-1", 50) } returns sample()
            setup()
            val res =
                client.get("/api/v1/recommendations?limit=999") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/recommendations")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
