package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.BillingInterval
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.feature.subscription.BillingRunResponse
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.feature.subscription.DigitalDownloadResponse
import com.piashcse.feature.subscription.DigitalLibraryItem
import com.piashcse.feature.subscription.SubscriptionPlanResponse
import com.piashcse.feature.subscription.SubscriptionResponse
import com.piashcse.feature.subscription.SubscriptionService
import com.piashcse.feature.subscription.digitalLibraryRoutes
import com.piashcse.feature.subscription.subscriptionAdminRoutes
import com.piashcse.feature.subscription.subscriptionPlanAdminRoutes
import com.piashcse.feature.subscription.subscriptionPlanRoutes
import com.piashcse.feature.subscription.subscriptionRoutes
import com.piashcse.plugin.adminAuth
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

class SubscriptionRoutesTest {
    private val subscriptions: SubscriptionService = mockk()
    private val deliveries: DigitalDeliveryService = mockk()

    private fun plan() =
        SubscriptionPlanResponse("plan-1", "Pro", "Digital access", "prod-1", BillingInterval.MONTH, "9.99", "USD", 7, true)

    private fun subscription() =
        SubscriptionResponse(
            "sub-1",
            "user-1",
            "plan-1",
            "Pro",
            "prod-1",
            SubscriptionStatus.ACTIVE,
            "2026-05-01T00:00:00Z",
            "2026-05-01T00:00:00Z",
            "2026-06-01T00:00:00Z",
            false,
            null,
        )

    private fun item() = DigitalLibraryItem("ent-1", "prod-1", "Pro Pack", "pack.zip", "tok-1", 0, 50, 50, null, true, null, "sub-1")

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single { subscriptions }
                    single { deliveries }
                },
            )
            routing {
                route("/api/v1/subscription-plans") { subscriptionPlanRoutes() }
                route("/api/v1/subscriptions") { subscriptionRoutes() }
                route("/api/v1/library") { digitalLibraryRoutes() }
                route("/api/v1/admin/subscription-plans") { adminAuth { subscriptionPlanAdminRoutes() } }
                route("/api/v1/admin/subscriptions") { adminAuth { subscriptionAdminRoutes() } }
            }
        }
    }

    @Test
    fun `plans are public`() =
        testApplication {
            coEvery { subscriptions.plans(null) } returns listOf(plan())
            setup()
            val res = client.get("/api/v1/subscription-plans")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "plan-1")
        }

    @Test
    fun `customer subscribes`() =
        testApplication {
            coEvery { subscriptions.subscribe("user-1", any()) } returns subscription()
            setup()
            val res =
                client.post("/api/v1/subscriptions") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"planId":"plan-1"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "sub-1")
        }

    @Test
    fun `customer cancels at period end`() =
        testApplication {
            coEvery { subscriptions.cancel("user-1", "sub-1") } returns subscription().copy(cancelAtPeriodEnd = true)
            setup()
            val res =
                client.post("/api/v1/subscriptions/sub-1/cancel") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "cancelAtPeriodEnd")
        }

    @Test
    fun `library lists entitlements and redeems downloads`() =
        testApplication {
            coEvery { deliveries.library("user-1") } returns listOf(item())
            coEvery { deliveries.download("tok-1", "user-1") } returns
                DigitalDownloadResponse("local://digital/prod-1", "pack.zip", 49)
            setup()
            val library =
                client.get("/api/v1/library") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, library.status)
            val download =
                client.get("/api/v1/library/tok-1/download") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, download.status)
            assertContains(download.bodyAsText(), "49")
        }

    @Test
    fun `admin creates a plan and runs billing`() =
        testApplication {
            coEvery { subscriptions.createPlan(any()) } returns plan()
            coEvery { subscriptions.runBilling(any()) } returns BillingRunResponse(2, 1)
            setup()
            val created =
                client.post("/api/v1/admin/subscription-plans") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Pro","description":"Digital","productId":"prod-1","interval":"MONTH","price":9.99}""")
                }
            assertEquals(HttpStatusCode.Created, created.status)
            val billed =
                client.post("/api/v1/admin/subscriptions/run-billing") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, billed.status)
            assertContains(billed.bodyAsText(), "\"invoicesCreated\": 2")
        }

    @Test
    fun `customers cannot run billing`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/subscriptions/run-billing") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
