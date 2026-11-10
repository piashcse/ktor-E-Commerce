package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.warehouse.WarehouseRepository
import com.piashcse.feature.warehouse.WarehouseResponse
import com.piashcse.feature.warehouse.warehouseSellerRoutes
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

class WarehouseRoutesTest {
    private val repo: WarehouseRepository = mockk()

    private fun sample() =
        WarehouseResponse(
            id = "wh-1",
            shopId = "shop-1",
            name = "Main",
            code = "WH1",
            address = null,
            city = null,
            country = null,
            isActive = true,
            createdAt = "2024-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { repo } })
            routing { route("/api/v1/seller/warehouses") { sellerAuth { warehouseSellerRoutes() } } }
        }
    }

    @Test
    fun `list returns warehouses`() =
        testApplication {
            coEvery { repo.list("user-1") } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/seller/warehouses") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "WH1")
        }

    @Test
    fun `create returns 201`() =
        testApplication {
            coEvery { repo.create("user-1", any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/seller/warehouses") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Main","code":"WH1"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `deactivate returns acknowledgement`() =
        testApplication {
            coEvery { repo.deactivate("user-1", "wh-1") } returns true
            setup()
            val res =
                client.delete("/api/v1/seller/warehouses/wh-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "deactivated")
        }

    @Test
    fun `customer is forbidden`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/seller/warehouses") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
