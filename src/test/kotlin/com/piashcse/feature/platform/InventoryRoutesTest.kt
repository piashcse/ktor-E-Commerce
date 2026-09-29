package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.InventoryStatus
import com.piashcse.feature.inventory.InventoryRepository
import com.piashcse.feature.inventory.inventorySellerRoutes
import com.piashcse.model.response.InventoryResponse
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

class InventoryRoutesTest {
    private val repo: InventoryRepository = mockk()

    private fun sample() =
        InventoryResponse(
            id = "i-1",
            productId = "p-1",
            shopId = "s-1",
            stockQuantity = 50,
            reservedQuantity = 0,
            minimumStockLevel = 5,
            maximumStockLevel = 100,
            status = InventoryStatus.IN_STOCK,
            lastRestocked = null,
            createdAt = null,
            updatedAt = null,
        )

    private fun validBody() =
        """{"productId":"p-1","shopId":"s-1","stockQuantity":50,"reservedQuantity":0,"minimumStockLevel":5,"maximumStockLevel":100}"""

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<InventoryRepository> { repo } })
            routing { route("/api/v1/seller/inventories") { sellerAuth { inventorySellerRoutes() } } }
        }
    }

    @Test
    fun `create-or-update happy path returns 201`() =
        testApplication {
            coEvery { repo.createOrUpdateInventory(any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/seller/inventories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "p-1")
        }

    @Test
    fun `create-or-update without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/inventories") {
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `create-or-update invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/inventories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `update-stock happy path returns 200`() =
        testApplication {
            coEvery { repo.updateStock("p-1", 5, "add") } returns sample().copy(stockQuantity = 55)
            setup()
            val res =
                client.put("/api/v1/seller/inventories/stock/p-1?quantity=5&operation=add") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "55")
        }

    @Test
    fun `update-stock without token returns 401`() =
        testApplication {
            setup()
            val res = client.put("/api/v1/seller/inventories/stock/p-1?quantity=5&operation=add")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `update-stock missing quantity returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/seller/inventories/stock/p-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `update-stock non-integer quantity returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/seller/inventories/stock/p-1?quantity=abc") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `get by product happy path returns 200`() =
        testApplication {
            coEvery { repo.getInventoryByProduct("p-1") } returns sample()
            setup()
            val res =
                client.get("/api/v1/seller/inventories/product/p-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "p-1")
        }

    @Test
    fun `get by product missing returns 404`() =
        testApplication {
            coEvery { repo.getInventoryByProduct("missing") } returns null
            setup()
            val res =
                client.get("/api/v1/seller/inventories/product/missing") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.NotFound, res.status)
        }

    @Test
    fun `get by product without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/seller/inventories/product/p-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `get by shop happy path returns 200`() =
        testApplication {
            coEvery { repo.getInventoryByShop(any(), any(), any()) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 10, 0))
            setup()
            val res =
                client.get("/api/v1/seller/inventories/shop/s-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "s-1")
        }

    @Test
    fun `low-stock happy path returns 200`() =
        testApplication {
            coEvery { repo.getLowStockProducts(any(), any()) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 10, 0))
            setup()
            val res =
                client.get("/api/v1/seller/inventories/low-stock") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `low-stock without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/seller/inventories/low-stock")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
