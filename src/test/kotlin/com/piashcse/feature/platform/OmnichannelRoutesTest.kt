package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PickupStatus
import com.piashcse.constants.PosSessionStatus
import com.piashcse.feature.omnichannel.OmnichannelService
import com.piashcse.feature.omnichannel.PosSessionResponse
import com.piashcse.feature.omnichannel.StoreResponse
import com.piashcse.feature.omnichannel.pickupActionRoutes
import com.piashcse.feature.omnichannel.pickupSellerListRoutes
import com.piashcse.feature.omnichannel.posSellerRoutes
import com.piashcse.feature.omnichannel.posSessionSellerRoutes
import com.piashcse.feature.omnichannel.storeLocatorRoutes
import com.piashcse.model.response.OrderResponse
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

class OmnichannelRoutesTest {
    private val service: OmnichannelService = mockk()

    private fun store() =
        StoreResponse(
            id = "store-1",
            shopId = "shop-1",
            name = "Dhaka Store",
            address = "12 Main St",
            city = "Dhaka",
            country = "BD",
            latitude = "23.810300",
            longitude = "90.412500",
            phone = "+880",
            openingHours = "9-5",
            supportsPickup = true,
            supportsPos = true,
            distanceKm = 1.2,
        )

    private fun order() =
        OrderResponse(
            orderId = "order-1",
            orderNumber = "PICK-1",
            subTotal = "10.00",
            total = "10.00",
            status = OrderStatus.PAID,
            pickupCode = "ABC123",
            pickupStatus = PickupStatus.READY,
        )

    private fun session() =
        PosSessionResponse(
            id = "session-1",
            shopId = "shop-1",
            warehouseId = "store-1",
            openedBy = "seller-user",
            status = PosSessionStatus.OPEN,
            openingFloat = "100.00",
            cashSales = "21.00",
            cardSales = "0.00",
            expectedCash = null,
            countedCash = null,
            variance = null,
            openedAt = "2026-06-01T00:00:00Z",
            closedAt = null,
            notes = null,
            closeNotes = null,
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/stores") { storeLocatorRoutes() }
                route("/api/v1/seller") {
                    route("orders") { sellerAuth { pickupActionRoutes() } }
                    route("pickup-orders") { sellerAuth { pickupSellerListRoutes() } }
                    route("pos/sales") { sellerAuth { posSellerRoutes() } }
                    route("pos/sessions") { sellerAuth { posSessionSellerRoutes() } }
                }
            }
        }
    }

    @Test
    fun `store locator is public`() =
        testApplication {
            coEvery { service.stores(null, null, null, 20) } returns listOf(store())
            setup()
            val res = client.get("/api/v1/stores")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Dhaka Store")
        }

    @Test
    fun `seller lists pickup orders`() =
        testApplication {
            coEvery { service.pickupOrders("seller-user", 20, 0) } returns
                PaginatedResponse(listOf(order()), PaginationMetadata(1, 20, 0))
            setup()
            val res =
                client.get("/api/v1/seller/pickup-orders") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "PICK-1")
        }

    @Test
    fun `seller marks pickup ready`() =
        testApplication {
            coEvery { service.markReady("seller-user", "order-1") } returns
                order().copy(pickupStatus = PickupStatus.READY)
            setup()
            val res =
                client.post("/api/v1/seller/orders/order-1/pickup/ready") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "READY")
        }

    @Test
    fun `seller completes pickup with code`() =
        testApplication {
            coEvery { service.completePickup("seller-user", "order-1", "ABC123") } returns
                order().copy(pickupStatus = PickupStatus.PICKED_UP, status = OrderStatus.RECEIVED)
            setup()
            val res =
                client.post("/api/v1/seller/orders/order-1/pickup/complete") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"pickupCode":"ABC123"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "PICKED_UP")
        }

    @Test
    fun `seller opens lists and closes POS sessions`() =
        testApplication {
            coEvery { service.openPosSession("seller-user", any()) } returns session()
            coEvery { service.posSessions("seller-user", 20, 0) } returns
                PaginatedResponse(listOf(session()), PaginationMetadata(1, 20, 0))
            coEvery { service.closePosSession("seller-user", "session-1", any()) } returns
                session().copy(status = PosSessionStatus.CLOSED, expectedCash = "121.00", countedCash = "121.00", variance = "0.00")
            setup()
            val opened =
                client.post("/api/v1/seller/pos/sessions/open") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"openingFloat":100.00,"warehouseId":"store-1"}""")
                }
            assertEquals(HttpStatusCode.Created, opened.status)
            val listed =
                client.get("/api/v1/seller/pos/sessions") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, listed.status)
            val closed =
                client.post("/api/v1/seller/pos/sessions/session-1/close") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"countedCash":121.00}""")
                }
            assertEquals(HttpStatusCode.OK, closed.status)
            assertContains(closed.bodyAsText(), "CLOSED")
        }

    @Test
    fun `seller creates and lists POS sales`() =
        testApplication {
            coEvery { service.posSale("seller-user", any()) } returns order().copy(fulfillmentType = FulfillmentType.POS)
            coEvery { service.posSales("seller-user", 20, 0) } returns
                PaginatedResponse(listOf(order()), PaginationMetadata(1, 20, 0))
            setup()
            val created =
                client.post("/api/v1/seller/pos/sales") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"items":[{"productId":"p-1","quantity":2}],"paymentMethod":"COD"}""")
                }
            assertEquals(HttpStatusCode.Created, created.status)
            val listed =
                client.get("/api/v1/seller/pos/sales") {
                    header(HttpHeaders.Authorization, authHeader(userId = "seller-user", userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, listed.status)
        }
}
