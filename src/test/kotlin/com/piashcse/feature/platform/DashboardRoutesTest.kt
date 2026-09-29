package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.dashboard.DashboardRepository
import com.piashcse.feature.dashboard.dashboardAdminRoutes
import com.piashcse.feature.dashboard.dashboardSellerRoutes
import com.piashcse.model.response.DailySignupEntry
import com.piashcse.model.response.DashboardStatsResponse
import com.piashcse.model.response.OrderStatsResponse
import com.piashcse.model.response.RecentActivityResponse
import com.piashcse.model.response.RevenueStatsResponse
import com.piashcse.model.response.TopProductResponse
import com.piashcse.model.response.UserGrowthResponse
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

class DashboardRoutesTest {
    private val repo: DashboardRepository = mockk()

    private fun stats() =
        DashboardStatsResponse(
            revenue = mapOf("total" to "1000.00"),
            orders = mapOf("total" to 5L),
            users = mapOf("total" to 10L),
            products = mapOf("total" to 20L),
            shops = mapOf("total" to 2L),
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<DashboardRepository> { repo } })
            routing {
                route("/api/v1/admin/dashboard") { adminAuth { dashboardAdminRoutes() } }
                route("/api/v1/seller/dashboard") { sellerAuth { dashboardSellerRoutes() } }
            }
        }
    }

    @Test
    fun `admin stats happy path returns 200`() =
        testApplication {
            coEvery { repo.getDashboardStats() } returns stats()
            setup()
            val res =
                client.get("/api/v1/admin/dashboard") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "1000.00")
        }

    @Test
    fun `admin stats without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/dashboard")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin stats with customer token returns 403`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/dashboard") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin revenue happy path returns 200`() =
        testApplication {
            coEvery { repo.getRevenueStats(any(), any()) } returns
                RevenueStatsResponse(
                    totalRevenue = "1000.00",
                    totalOrders = 50L,
                    averageOrderValue = "20.00",
                    dailyRevenue = listOf(mapOf("date" to "2024-01-01", "revenue" to "100.00")),
                )
            setup()
            val res =
                client.get("/api/v1/admin/dashboard/revenue") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "1000.00")
        }

    @Test
    fun `admin revenue without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/dashboard/revenue")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin orders happy path returns 200`() =
        testApplication {
            coEvery { repo.getOrderStats(any()) } returns
                OrderStatsResponse(
                    statusDistribution = mapOf("PENDING" to 3L),
                    recentOrders = listOf(mapOf("id" to "o-1")),
                )
            setup()
            val res =
                client.get("/api/v1/admin/dashboard/orders") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "PENDING")
        }

    @Test
    fun `admin users happy path returns 200`() =
        testApplication {
            coEvery { repo.getUserGrowth(any()) } returns
                UserGrowthResponse(
                    totalUsers = 100L,
                    newUsersInPeriod = 10L,
                    periodDays = 7,
                    byUserType = mapOf("CUSTOMER" to 90L),
                    dailySignups = listOf(DailySignupEntry("2024-01-01", 5L)),
                )
            setup()
            val res =
                client.get("/api/v1/admin/dashboard/users") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "100")
        }

    @Test
    fun `admin top-products happy path returns 200`() =
        testApplication {
            coEvery { repo.getTopProducts(any()) } returns
                listOf(
                    TopProductResponse("p-1", "Widget", "W-1", 10, "500.00", 20, "4.5", "ACTIVE"),
                )
            setup()
            val res =
                client.get("/api/v1/admin/dashboard/top-products") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Widget")
        }

    @Test
    fun `admin activity happy path returns 200`() =
        testApplication {
            coEvery { repo.getRecentActivity(any()) } returns
                listOf(
                    RecentActivityResponse("a-1", "ORDER", "New order placed", "PENDING", "2024-01-01T00:00:00"),
                )
            setup()
            val res =
                client.get("/api/v1/admin/dashboard/activity") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "New order placed")
        }

    @Test
    fun `seller stats happy path returns 200`() =
        testApplication {
            coEvery { repo.getSellerStats("user-1") } returns stats()
            setup()
            val res =
                client.get("/api/v1/seller/dashboard") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "1000.00")
        }

    @Test
    fun `seller stats without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/seller/dashboard")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
