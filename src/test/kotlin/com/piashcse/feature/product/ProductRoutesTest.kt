package com.piashcse.feature.product

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.ProductStatus
import com.piashcse.model.response.FacetCount
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.SearchFacets
import com.piashcse.model.response.SearchResponse
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
import com.piashcse.service.NoOpCache
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
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

class ProductRoutesTest {
    private val productRepo: ProductRepository = mockk()

    private fun sampleProduct() =
        ProductResponse(
            id = "prod-1", userId = "user-1", shopId = "shop-1", categoryId = "cat-1",
            subCategoryId = "sub-1", brandId = "brand-1", name = "Phone", description = "Nice phone",
            sku = "SKU1", barcode = "123", weight = 0.5, dimensions = "10x5", minOrderQuantity = 1,
            price = "99.99", discountPrice = "79.99", discountPercentage = 20.0, videoLink = null,
            hotDeal = false, featured = true, bestSeller = false, newProduct = true,
            freeShipping = true, images = listOf("img1.jpg"), status = ProductStatus.ACTIVE,
            viewCount = 5, rating = 4.5, totalReviews = 2, totalSales = 10,
        )

    private fun pageOfProducts() = PaginatedResponse(listOf(sampleProduct()), PaginationMetadata(totalCount = 1, limit = 10, offset = 0))

    private fun sampleSearch() =
        SearchResponse(
            products = listOf(sampleProduct()),
            metadata = PaginationMetadata(totalCount = 1, limit = 10, offset = 0),
            facets = SearchFacets(categories = listOf(FacetCount("cat-1", "Electronics", 1)), brands = emptyList()),
        )

    private fun validProductJson() =
        """
        {"categoryId":"cat-1","subCategoryId":"sub-1","brandId":"brand-1","name":"Phone",
        "description":"Nice phone","productCode":"P1","stockQuantity":10,"price":99.99,
        "discountPrice":79.99,"status":1,"videoLink":null,"hotDeal":false,"featured":false,
        "freeShipping":true,"images":["img1.jpg"]}
        """.trimIndent()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single<ProductRepository> { productRepo }
                    single { ProductCatalogService(get(), NoOpCache) }
                    single { ProductCrudService(get(), NoOpCache) }
                },
            )
            routing {
                route("/api/v1/products") { productRoutes() }
                route("/api/v1/seller/products") { sellerAuth { productSellerRoutes() } }
                route("/api/v1/admin/products") { adminAuth { productAdminRoutes() } }
            }
        }
    }

    // ─── Public product discovery ───

    @Test
    fun `product detail is public returns 200`() =
        testApplication {
            coEvery { productRepo.incrementViewCount(any()) } returns Unit
            coEvery { productRepo.getProductDetail("prod-1") } returns sampleProduct()
            setup()
            val res = client.get("/api/v1/products/prod-1")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Phone")
        }

    @Test
    fun `product list is public returns 200`() =
        testApplication {
            coEvery { productRepo.getProducts(any()) } returns pageOfProducts()
            setup()
            val res = client.get("/api/v1/products")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Phone")
        }

    @Test
    fun `product list invalid pagination returns 400`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/products?perPage=not-a-number")
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `product search happy path returns 200`() =
        testApplication {
            coEvery { productRepo.searchProduct(any()) } returns sampleSearch()
            setup()
            val res = client.get("/api/v1/products/search?name=phone")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Phone")
        }

    @Test
    fun `product search missing name returns 400`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/products/search")
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    // ─── Seller product management (SELLER role) ───

    @Test
    fun `seller products without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/seller/products")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `seller products happy path returns 200`() =
        testApplication {
            coEvery { productRepo.getProductsByUser(any(), any()) } returns pageOfProducts()
            setup()
            val res =
                client.get("/api/v1/seller/products") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Phone")
        }

    @Test
    fun `seller create product happy path returns 201`() =
        testApplication {
            coEvery { productRepo.createProduct(any(), any(), any()) } returns sampleProduct()
            setup()
            val res =
                client.post("/api/v1/seller/products") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody(validProductJson())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Phone")
        }

    @Test
    fun `seller create product without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/products") {
                    contentType(ContentType.Application.Json)
                    setBody(validProductJson())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `seller create product invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/products") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody(validProductJson().replace("\"name\":\"Phone\"", "\"name\":\"\"").replace("99.99", "0"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `seller create product with customer token returns 403`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/products") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody(validProductJson())
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `seller update product happy path returns 200`() =
        testApplication {
            coEvery { productRepo.updateProduct(any(), any(), any()) } returns sampleProduct().copy(name = "Phone Pro")
            setup()
            val res =
                client.put("/api/v1/seller/products/prod-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody(
                        """{"categoryId":null,"subCategoryId":null,"brandId":null,"name":"Phone Pro",
                "description":null,"price":null,"discountPrice":null,"status":null,"videoLink":null,
                "hotDeal":null,"featured":null,"freeShipping":null,"images":[]}""",
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Phone Pro")
        }

    @Test
    fun `seller update product empty body returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/seller/products/prod-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `seller delete product happy path returns 200`() =
        testApplication {
            coEvery { productRepo.deleteProduct(any(), any()) } returns "Product deleted successfully"
            setup()
            val res =
                client.delete("/api/v1/seller/products/prod-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "deleted")
        }

    @Test
    fun `seller delete product without token returns 401`() =
        testApplication {
            setup()
            val res = client.delete("/api/v1/seller/products/prod-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `image upload without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/products/image-upload") {
                    setBody(MultiPartFormDataContent(formData { }))
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `image upload empty multipart returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/seller/products/image-upload") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    setBody(MultiPartFormDataContent(formData { }))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    // ─── Admin product management (ADMIN role) ───

    @Test
    fun `admin delete product happy path returns 200`() =
        testApplication {
            coEvery { productRepo.deleteProductAsAdmin("prod-1") } returns "prod-1"
            setup()
            val res =
                client.delete("/api/v1/admin/products/prod-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "prod-1")
        }

    @Test
    fun `admin delete product without token returns 401`() =
        testApplication {
            setup()
            val res = client.delete("/api/v1/admin/products/prod-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin delete product with seller token returns 403`() =
        testApplication {
            setup()
            val res =
                client.delete("/api/v1/admin/products/prod-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
