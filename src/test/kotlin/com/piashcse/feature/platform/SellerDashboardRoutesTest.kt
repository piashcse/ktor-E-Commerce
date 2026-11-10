package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.dashboard.DashboardRepository
import com.piashcse.feature.dashboard.dashboardSellerRoutes
import com.piashcse.model.response.DashboardStatsResponse
import com.piashcse.plugin.sellerAuth
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

class SellerDashboardRoutesTest {
    private val repo: DashboardRepository = mockk()

    private fun sample() =
        DashboardStatsResponse(
            revenue = mapOf("total" to "100.00", "today" to "10.00"),
            orders = mapOf("total" to 2L),
            users = mapOf("total" to 1L),
            products = mapOf("total" to 3L),
            shops = mapOf("total" to 1L),
        )

    private fun io.ktor.server.testing.ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<DashboardRepository> { repo } })
            routing { route("/api/v1/seller/dashboard") { sellerAuth { dashboardSellerRoutes() } } }
        }
    }

    @Test
    fun `seller sees own dashboard`() =
        testApplication {
            coEvery { repo.getSellerStats("user-1") } returns sample()
            setup()
            val res =
                client.get("/api/v1/seller/dashboard") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "100.00")
        }

    @Test
    fun `customer is forbidden`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/seller/dashboard") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/seller/dashboard").status)
        }
}
