package com.piashcse.feature.catalog

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.brand.BrandRepository
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.product_category.ProductCategoryRepository
import com.piashcse.feature.product_category.productCategoryAdminRoutes
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.ProductSubCategoryRepository
import com.piashcse.feature.product_sub_category.productSubCategoryAdminRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.shop_category.ShopCategoryRepository
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.model.response.BrandResponse
import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.plugin.adminAuth
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

class CatalogRoutesTest {
    private val brandRepo: BrandRepository = mockk()
    private val productCategoryRepo: ProductCategoryRepository = mockk()
    private val productSubCategoryRepo: ProductSubCategoryRepository = mockk()
    private val shopCategoryRepo: ShopCategoryRepository = mockk()

    private fun sampleBrand() = BrandResponse(id = "brand-1", name = "Nike", logo = null)

    private fun sampleSubCategory() = ProductSubCategoryResponse(id = "sub-1", categoryId = "cat-1", name = "Phones", image = null)

    private fun sampleCategory() =
        ProductCategoryResponse(id = "cat-1", name = "Electronics", subCategories = listOf(sampleSubCategory()), image = null)

    private fun sampleShopCategory() = ShopCategoryResponse(id = "scat-1", name = "Fashion")

    private fun <T> pageOf(item: T) = PaginatedResponse(listOf(item), PaginationMetadata(totalCount = 1, limit = 20, offset = 0))

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single<BrandRepository> { brandRepo }
                    single<ProductCategoryRepository> { productCategoryRepo }
                    single<ProductSubCategoryRepository> { productSubCategoryRepo }
                    single<ShopCategoryRepository> { shopCategoryRepo }
                },
            )
            routing {
                route("/api/v1/brands") { brandRoutes() }
                route("/api/v1/admin/brands") { adminAuth { brandAdminRoutes() } }
                route("/api/v1/product-categories") { productCategoryRoutes() }
                route("/api/v1/admin/product-categories") { adminAuth { productCategoryAdminRoutes() } }
                route("/api/v1/product-subcategories") { productSubCategoryRoutes() }
                route("/api/v1/admin/product-subcategories") { adminAuth { productSubCategoryAdminRoutes() } }
                route("/api/v1/admin/shop-categories") { adminAuth { shopCategoryAdminRoutes() } }
            }
        }
    }

    // ─── Brand (GET requires CUSTOMER+; writes are admin-only) ───

    @Test
    fun `brand list without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/brands")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `brand list happy path returns 200`() =
        testApplication {
            coEvery { brandRepo.getBrands(any(), any()) } returns pageOf(sampleBrand())
            setup()
            val res =
                client.get("/api/v1/brands") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Nike")
        }

    @Test
    fun `admin create brand happy path returns 201`() =
        testApplication {
            coEvery { brandRepo.createBrand("Nike") } returns sampleBrand()
            setup()
            val res =
                client.post("/api/v1/admin/brands") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Nike"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Nike")
        }

    @Test
    fun `admin create brand without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/brands") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Nike"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin create brand blank name returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/brands") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":""}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin brand write with customer token returns 403`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/brands") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Nike"}""")
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin update brand happy path returns 200`() =
        testApplication {
            coEvery { brandRepo.updateBrand("brand-1", "Adidas") } returns sampleBrand().copy(name = "Adidas")
            setup()
            val res =
                client.put("/api/v1/admin/brands/brand-1?name=Adidas") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Adidas")
        }

    @Test
    fun `admin update brand missing name returns 400`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/admin/brands/brand-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin delete brand happy path returns 200`() =
        testApplication {
            coEvery { brandRepo.deleteBrand("brand-1") } returns "Brand deleted successfully"
            setup()
            val res =
                client.delete("/api/v1/admin/brands/brand-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "deleted")
        }

    // ─── Product category (public GET; writes are admin-only) ───

    @Test
    fun `product category list is public returns 200`() =
        testApplication {
            coEvery { productCategoryRepo.getCategories(any(), any()) } returns pageOf(sampleCategory())
            setup()
            val res = client.get("/api/v1/product-categories")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Electronics")
        }

    @Test
    fun `admin create product category happy path returns 201`() =
        testApplication {
            coEvery { productCategoryRepo.createCategory("Electronics") } returns sampleCategory()
            setup()
            val res =
                client.post("/api/v1/admin/product-categories?name=Electronics") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Electronics")
        }

    @Test
    fun `admin create product category without token returns 401`() =
        testApplication {
            setup()
            val res = client.post("/api/v1/admin/product-categories?name=Electronics")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin create product category missing name returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/product-categories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin update product category happy path returns 200`() =
        testApplication {
            coEvery { productCategoryRepo.updateCategory("cat-1", "Gadgets") } returns sampleCategory().copy(name = "Gadgets")
            setup()
            val res =
                client.put("/api/v1/admin/product-categories/cat-1?name=Gadgets") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Gadgets")
        }

    @Test
    fun `admin delete product category happy path returns 200`() =
        testApplication {
            coEvery { productCategoryRepo.deleteCategory("cat-1") } returns "Category deleted successfully"
            setup()
            val res =
                client.delete("/api/v1/admin/product-categories/cat-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    // ─── Product subcategory (public GET needs categoryId; writes are admin-only) ───

    @Test
    fun `product subcategory list happy path returns 200`() =
        testApplication {
            coEvery { productSubCategoryRepo.getProductSubCategory(any(), any(), any()) } returns pageOf(sampleSubCategory())
            setup()
            val res = client.get("/api/v1/product-subcategories?categoryId=cat-1")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Phones")
        }

    @Test
    fun `product subcategory list missing categoryId returns 400`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/product-subcategories")
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin create subcategory happy path returns 201`() =
        testApplication {
            coEvery { productSubCategoryRepo.addProductSubCategory(any()) } returns sampleSubCategory()
            setup()
            val res =
                client.post("/api/v1/admin/product-subcategories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"categoryId":"cat-1","name":"Phones"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Phones")
        }

    @Test
    fun `admin create subcategory without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/product-subcategories") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"categoryId":"cat-1","name":"Phones"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin create subcategory invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/product-subcategories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"categoryId":"","name":""}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin update subcategory happy path returns 200`() =
        testApplication {
            coEvery { productSubCategoryRepo.updateProductSubCategory("sub-1", "Mobiles") } returns
                sampleSubCategory().copy(name = "Mobiles")
            setup()
            val res =
                client.put("/api/v1/admin/product-subcategories/sub-1?name=Mobiles") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Mobiles")
        }

    @Test
    fun `admin delete subcategory happy path returns 200`() =
        testApplication {
            coEvery { productSubCategoryRepo.deleteProductSubCategory("sub-1") } returns "sub-1"
            setup()
            val res =
                client.delete("/api/v1/admin/product-subcategories/sub-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    // ─── Shop category (admin-only; no public read route) ───

    @Test
    fun `admin create shop category happy path returns 201`() =
        testApplication {
            coEvery { shopCategoryRepo.createCategory("Fashion") } returns sampleShopCategory()
            setup()
            val res =
                client.post("/api/v1/admin/shop-categories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Fashion"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Fashion")
        }

    @Test
    fun `admin create shop category without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/shop-categories") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Fashion"}""")
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `admin create shop category blank name returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/shop-categories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":""}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `admin shop category write with seller token returns 403`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/shop-categories") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Fashion"}""")
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin update shop category happy path returns 200`() =
        testApplication {
            coEvery { shopCategoryRepo.updateCategory("scat-1", "Apparel") } returns sampleShopCategory().copy(name = "Apparel")
            setup()
            val res =
                client.put("/api/v1/admin/shop-categories/scat-1?name=Apparel") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Apparel")
        }

    @Test
    fun `admin delete shop category happy path returns 200`() =
        testApplication {
            coEvery { shopCategoryRepo.deleteCategory("scat-1") } returns "Shop category deleted successfully"
            setup()
            val res =
                client.delete("/api/v1/admin/shop-categories/scat-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}
