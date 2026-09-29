package com.piashcse.feature.catalog

import com.piashcse.model.request.BrandRequest
import com.piashcse.model.request.ProductCategoryRequest
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.request.ShopCategoryRequest
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.request.UpdateShopRequest
import org.valiktor.ConstraintViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogValidationTest {
    @Test
    fun `BrandRequest accepts non-blank name`() {
        assertEquals("Nike", BrandRequest("Nike").name)
    }

    @Test
    fun `BrandRequest rejects blank name`() {
        assertFailsWith<ConstraintViolationException> { BrandRequest("") }
    }

    @Test
    fun `ShopRequest accepts valid payload`() {
        val req =
            ShopRequest(
                name = "Tech Store",
                categoryId = "scat-1",
                description = null,
                address = null,
                phone = null,
                email = null,
                logo = null,
                coverImage = null,
            )
        assertEquals("Tech Store", req.name)
    }

    @Test
    fun `ShopRequest rejects blank name`() {
        assertFailsWith<ConstraintViolationException> {
            ShopRequest(
                name = "",
                categoryId = "scat-1",
                description = null,
                address = null,
                phone = null,
                email = null,
                logo = null,
                coverImage = null,
            )
        }
    }

    @Test
    fun `ShopRequest rejects blank categoryId`() {
        assertFailsWith<ConstraintViolationException> {
            ShopRequest(
                name = "Tech Store",
                categoryId = "",
                description = null,
                address = null,
                phone = null,
                email = null,
                logo = null,
                coverImage = null,
            )
        }
    }

    @Test
    fun `UpdateShopRequest allows all-null partial update`() {
        assertEquals(null, UpdateShopRequest().name)
    }

    @Test
    fun `ProductCategoryRequest accepts non-blank name`() {
        assertEquals("Electronics", ProductCategoryRequest("Electronics").name)
    }

    @Test
    fun `ProductCategoryRequest rejects blank name`() {
        assertFailsWith<ConstraintViolationException> { ProductCategoryRequest("") }
    }

    @Test
    fun `ProductSubCategoryRequest accepts valid payload`() {
        assertEquals("Phones", ProductSubCategoryRequest(categoryId = "cat-1", name = "Phones").name)
    }

    @Test
    fun `ProductSubCategoryRequest rejects blank fields`() {
        assertFailsWith<ConstraintViolationException> { ProductSubCategoryRequest(categoryId = "", name = "Phones") }
        assertFailsWith<ConstraintViolationException> { ProductSubCategoryRequest(categoryId = "cat-1", name = "") }
    }

    @Test
    fun `ShopCategoryRequest accepts non-blank name`() {
        assertEquals("Fashion", ShopCategoryRequest("Fashion").name)
    }

    @Test
    fun `ShopCategoryRequest rejects blank name`() {
        assertFailsWith<ConstraintViolationException> { ShopCategoryRequest("") }
    }

    private fun validProductRequest() =
        ProductRequest(
            categoryId = "cat-1", subCategoryId = "sub-1", brandId = "brand-1",
            name = "Phone", description = "Nice phone", productCode = "P1",
            stockQuantity = 5, price = 99.99, discountPrice = 79.99, status = 1,
            videoLink = null, hotDeal = false, featured = true, freeShipping = true,
            images = listOf("img1.jpg"),
        )

    @Test
    fun `ProductRequest accepts valid payload`() {
        assertEquals("Phone", validProductRequest().name)
    }

    @Test
    fun `ProductRequest rejects blank name`() {
        assertFailsWith<ConstraintViolationException> { validProductRequest().copy(name = "") }
    }

    @Test
    fun `ProductRequest rejects blank description`() {
        assertFailsWith<ConstraintViolationException> { validProductRequest().copy(description = "") }
    }

    @Test
    fun `ProductRequest rejects blank categoryId`() {
        assertFailsWith<ConstraintViolationException> { validProductRequest().copy(categoryId = "") }
    }

    @Test
    fun `ProductRequest rejects non-positive price`() {
        assertFailsWith<ConstraintViolationException> { validProductRequest().copy(price = 0.0) }
        assertFailsWith<ConstraintViolationException> { validProductRequest().copy(price = -5.0) }
    }

    @Test
    fun `UpdateProductRequest accepts partial update`() {
        val req =
            UpdateProductRequest(
                categoryId = null, subCategoryId = null, brandId = null, name = "Phone Pro",
                description = null, price = 109.99, discountPrice = null, status = null,
                videoLink = null, hotDeal = null, featured = null, freeShipping = null,
                images = emptyList(),
            )
        assertEquals("Phone Pro", req.name)
    }

    @Test
    fun `ReviewRatingRequest accepts valid payload`() {
        assertEquals(5, ReviewRatingRequest(productId = "prod-1", reviewText = "Great", rating = 5).rating)
    }

    @Test
    fun `ReviewRatingRequest rejects out-of-range rating`() {
        assertFailsWith<ConstraintViolationException> {
            ReviewRatingRequest(productId = "prod-1", reviewText = "Great", rating = 0)
        }
        assertFailsWith<ConstraintViolationException> {
            ReviewRatingRequest(productId = "prod-1", reviewText = "Great", rating = 6)
        }
    }

    @Test
    fun `ReviewRatingRequest rejects blank fields`() {
        assertFailsWith<ConstraintViolationException> {
            ReviewRatingRequest(productId = "", reviewText = "Great", rating = 5)
        }
        assertFailsWith<ConstraintViolationException> {
            ReviewRatingRequest(productId = "prod-1", reviewText = "", rating = 5)
        }
    }

    @Test
    fun `ProductSearchRequest accepts valid payload`() {
        val req = ProductSearchRequest(limit = 10, offset = 0, name = "phone", maxPrice = null, minPrice = null, categoryId = null)
        assertEquals("phone", req.name)
    }

    @Test
    fun `ProductSearchRequest rejects blank name`() {
        assertFailsWith<ConstraintViolationException> {
            ProductSearchRequest(limit = 10, offset = 0, name = "", maxPrice = null, minPrice = null, categoryId = null)
        }
    }
}
