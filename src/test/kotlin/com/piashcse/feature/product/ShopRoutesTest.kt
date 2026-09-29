package com.piashcse.feature.product

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.ShopStatus
import com.piashcse.feature.shop.ShopRepository
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop.shopSellerRoutesV1
import com.piashcse.model.response.ShopResponse
import com.piashcse.plugin.adminAuth
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
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ShopRoutesTest {
    private val shopRepo: ShopRepository = mockk()

    private fun sampleShop() =
        ShopResponse(
            id = "shop-1", name = "Tech Store", categoryId = "scat-1", description = "Gadgets",
            address = "123 Main St", phone = "123456", email = "shop@example.com",
            logo = null, coverImage = null, status = ShopStatus.APPROVED,
            rating = BigDecimal("4.5"), totalReviews = 10,
            createdAt = LocalDateTime.of(2024, 1, 1, 0, 0), updatedAt = null,
        )

    private fun pageOfShops() = PaginatedResponse(listOf(sampleShop()), PaginationMetadata(totalCount = 1, limit = 20, offset = 0))

    private fun validShopJson() =
        """
        {"name":"Tech Store","categoryId":"scat-1","description":"Gadgets","address":"123 Main St",
        "phone":"123456","email":"shop@example.com","logo":null,"coverImage":null}
        """.trimIndent()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single<ShopRepository> { shopRepo }
                },
            )
            routing {
                route("/api/v1/shops") { shopRoutes() }
                route("/api/v1/seller/shops") { sellerAuth { shopSellerRoutesV1() } }
                route("/api/v1/admin/shops") { adminAuth { shopAdminRoutes() } }
            }
        }
    }

    // ─── Public + customer discovery ───

    @Test
    fun `get shop by id is public returns 200`() =
        testApplication {
            coEvery { shopRepo.getShopById("shop-1") } returns sampleShop()
            setup()
            val res = client.get("/api/v1/shops/shop-1")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    @Test
    fun `get shop by id missing returns 404`() =
        testApplication {
            coEvery { shopRepo.getShopById("nope") } returns null
            setup()
            val res = client.get("/api/v1/shops/nope")
            assertEquals(HttpStatusCode.NotFound, res.status)
        }

    @Test
    fun `public shops without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/shops/public")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `public shops happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.getShops(any(), any(), any(), any()) } returns pageOfShops()
            setup()
            val res =
                client.get("/api/v1/shops/public") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    @Test
    fun `shops by category happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.getShopsByCategory(any(), any(), any()) } returns pageOfShops()
            setup()
            val res =
                client.get("/api/v1/shops/category/scat-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    @Test
    fun `featured shops happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.getFeaturedShops(any(), any()) } returns pageOfShops()
            setup()
            val res =
                client.get("/api/v1/shops/featured") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    // ─── Seller shop management (SELLER role) ───

    @Test
    fun `seller create shop happy path returns 201`() =
        testApplication {
            coEvery { shopRepo.createShop(any(), any()) } returns sampleShop()
            setup()
            val res =
                client.post("/api/v1/seller/shops") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody(validShopJson())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    @Test
    fun `seller create shop without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/shops") {
                    contentType(ContentType.Application.Json)
                    setBody(validShopJson())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `seller create shop invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/shops") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody(validShopJson().replace("\"name\":\"Tech Store\"", "\"name\":\"\""))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `seller shop write with customer token returns 403`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/shops") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody(validShopJson())
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `seller update shop happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.updateShop(any(), any(), any()) } returns sampleShop().copy(name = "Renamed Store")
            setup()
            val res =
                client.put("/api/v1/seller/shops/shop-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Renamed Store"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Renamed Store")
        }

    @Test
    fun `seller update shop malformed body returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/seller/shops/shop-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("{broken")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `seller owned shops without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/seller/shops")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `seller owned shops happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.getShopsByUser(any(), any(), any()) } returns pageOfShops()
            setup()
            val res =
                client.get("/api/v1/seller/shops") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    // ─── Admin shop moderation (ADMIN role) ───

    @Test
    fun `admin approve shop happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.approveShop(any(), any(), any(), any()) } returns sampleShop()
            setup()
            val res =
                client.put("/api/v1/admin/shops/approve/shop-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    @Test
    fun `admin approve shop without token returns 401`() =
        testApplication {
            setup()
            val res = client.put("/api/v1/admin/shops/approve/shop-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin reject shop happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.rejectShop(any(), any(), any(), any()) } returns sampleShop()
            setup()
            val res =
                client.put("/api/v1/admin/shops/reject/shop-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin suspend shop happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.suspendShop(any(), any(), any(), any()) } returns sampleShop()
            setup()
            val res =
                client.put("/api/v1/admin/shops/suspend/shop-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin activate shop happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.activateShop(any(), any(), any(), any()) } returns sampleShop()
            setup()
            val res =
                client.put("/api/v1/admin/shops/activate/shop-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `admin shops by status happy path returns 200`() =
        testApplication {
            coEvery { shopRepo.getShopsByStatus(any(), any(), any()) } returns pageOfShops()
            setup()
            val res =
                client.get("/api/v1/admin/shops/status?status=APPROVED") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Tech Store")
        }

    @Test
    fun `admin shops by status invalid enum returns 400`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/shops/status?status=BOGUS") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin shops by status missing param returns 400`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/shops/status") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin shops by status without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/shops/status?status=APPROVED")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
