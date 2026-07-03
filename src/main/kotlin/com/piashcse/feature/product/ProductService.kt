package com.piashcse.feature.product

import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.*
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.mapper.toProductResponse
import com.piashcse.service.CacheService
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode

class ProductService : ProductRepository {
    private val cache = CacheService.cache

    private suspend fun <T> cachedOrQuery(cacheKey: String, query: suspend () -> T): T {
        cache.get<T>(cacheKey)?.let { return it }
        return query().also { cache.set(cacheKey, it) }
    }

    override suspend fun createProduct(
        userId: String,
        shopId: String?,
        productRequest: ProductRequest,
    ): ProductResponse = query {
        requireSeller(userId)
        if (shopId != null) {
            val shop = ShopDAO.findById(shopId) ?: shopId.throwNotFound("Shop")
            if (shop.userId.value != userId) throw ForbiddenException("You are not the owner of this shop")
        }

        ProductDAO.new {
            this.userId = EntityID(userId, UserTable)
            this.shopId = shopId?.let { EntityID(shopId, ShopTable) }
            categoryId = EntityID(productRequest.categoryId, ProductCategoryTable)
            subCategoryId = productRequest.subCategoryId?.let { EntityID(it, ProductSubCategoryTable) }
            brandId = productRequest.brandId?.let { EntityID(it, BrandTable) }
            sku = generateSKU(productRequest.name)
            name = productRequest.name
            description = productRequest.description
            price = BigDecimal.valueOf(productRequest.price)
            discountPrice = productRequest.discountPrice?.let { BigDecimal.valueOf(it) }
            discountPercentage = calcDiscountPct(productRequest.price, productRequest.discountPrice)
            stockQuantity = productRequest.stockQuantity
            videoLink = productRequest.videoLink
            hotDeal = productRequest.hotDeal
            featured = productRequest.featured
            bestSeller = false
            newProduct = true
            freeShipping = productRequest.freeShipping ?: false
            status = ProductStatus.ACTIVE
        }.apply {
            setImages(productRequest.images)
        }.toProductResponse()
    }.also { cache.invalidatePattern("products:.*") }

    private fun requireSeller(userId: String) {
        if (findSellerByUserId(userId) == null) throw NotFoundException(Message.Errors.SELLER_REQUIRED)
    }

    private fun generateSKU(name: String) =
        name.replace(Regex("[^a-zA-Z0-9]"), "").take(6).uppercase() +
            System.currentTimeMillis().toString().takeLast(6)

    private fun calcDiscountPct(price: Double, discountPrice: Double?): BigDecimal? =
        if (discountPrice != null && discountPrice < price)
            BigDecimal.valueOf((price - discountPrice) / price * 100).setScale(2, RoundingMode.HALF_UP)
        else null

    override suspend fun updateProduct(
        userId: String,
        productId: String,
        updateProduct: UpdateProductRequest,
    ): ProductResponse = query {
        requireSeller(userId)
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
        product.verifyOwnership(userId, "product") { it.userId.value }

        product.apply {
            categoryId = updateProduct.categoryId?.let { EntityID(it, ProductCategoryTable) } ?: categoryId
            subCategoryId = updateProduct.subCategoryId?.let { EntityID(it, ProductSubCategoryTable) } ?: subCategoryId
            brandId = updateProduct.brandId?.let { EntityID(it, BrandTable) } ?: brandId
            name = updateProduct.name ?: name
            description = updateProduct.description ?: description
            price = updateProduct.price?.let { BigDecimal.valueOf(it) } ?: price
            discountPrice = updateProduct.discountPrice?.let { BigDecimal.valueOf(it) } ?: discountPrice
            stockQuantity = updateProduct.stockQuantity ?: stockQuantity
            videoLink = updateProduct.videoLink ?: videoLink
            hotDeal = updateProduct.hotDeal ?: hotDeal
            featured = updateProduct.featured ?: featured
            freeShipping = updateProduct.freeShipping ?: freeShipping
            if (updateProduct.images.isNotEmpty()) setImages(updateProduct.images)
        }.toProductResponse()
    }.also { cache.invalidatePattern("products:.*") }

    // ── Product listing with shared filter builder ─────────────────────────

    private fun Query.applyProductFilters(filter: ProductWithFilterRequest): Query {
        filter.categoryId?.let { andWhere { ProductTable.categoryId eq EntityID(it, ProductCategoryTable) } }
        filter.subCategoryId?.let { andWhere { ProductTable.subCategoryId eq EntityID(it, ProductSubCategoryTable) } }
        filter.brandId?.let { andWhere { ProductTable.brandId eq EntityID(it, BrandTable) } }
        filter.minPrice?.let { andWhere { ProductTable.price greaterEq BigDecimal.valueOf(it) } }
        filter.maxPrice?.let { andWhere { ProductTable.price lessEq BigDecimal.valueOf(it) } }
        val col = when (filter.sortBy?.lowercase()) {
            "price" -> ProductTable.price
            "name" -> ProductTable.name
            else -> ProductTable.createdAt
        }
        return orderBy(col to if (filter.sortOrder?.lowercase() == "asc") SortOrder.ASC else SortOrder.DESC)
    }

    override suspend fun getProducts(filter: ProductWithFilterRequest): PaginatedResponse<ProductResponse> = query {
        ProductTable.selectAll().andWhere { ProductTable.status eq ProductStatus.ACTIVE }
            .applyProductFilters(filter)
            .toPaginatedResponse(filter.limit, filter.offset) { ProductDAO.wrapRow(it).toProductResponse() }
    }

    override suspend fun getProductsByShop(shopId: String, filter: ProductWithFilterRequest): PaginatedResponse<ProductResponse> = query {
        ProductTable.selectAll().andWhere {
            (ProductTable.shopId eq shopId) and (ProductTable.status eq ProductStatus.ACTIVE)
        }.applyProductFilters(filter)
            .toPaginatedResponse(filter.limit, filter.offset) { ProductDAO.wrapRow(it).toProductResponse() }
    }

    override suspend fun getProductsByUser(userId: String, filter: ProductWithFilterRequest): PaginatedResponse<ProductResponse> = query {
        ProductTable.selectAll().andWhere {
            (ProductTable.userId eq userId) and (ProductTable.status eq ProductStatus.ACTIVE)
        }.applyProductFilters(filter)
            .toPaginatedResponse(filter.limit, filter.offset) { ProductDAO.wrapRow(it).toProductResponse() }
    }

    override suspend fun getProductDetail(productId: String): ProductResponse =
        cachedOrQuery("products:detail:$productId") {
            query {
                ProductDAO.findById(productId)?.toProductResponse() ?: productId.throwNotFound("ProductResponse")
            }
        }

    override suspend fun deleteProduct(userId: String, productId: String): String = query {
        requireSeller(userId)
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
        product.verifyOwnership(userId, "product") { it.userId.value }
        product.delete()
        productId
    }.also { cache.invalidatePattern("products:.*") }

    suspend fun deleteProductAsAdmin(productId: String): String = query {
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("ProductResponse")
        product.delete()
        productId
    }.also { cache.invalidatePattern("products:.*") }

    override suspend fun incrementViewCount(productId: String) = query {
        ProductDAO.findById(productId)?.apply { viewCount = viewCount + 1 }
        Unit
    }

    // ── Search ─────────────────────────────────────────────────────────────

    override suspend fun searchProduct(searchRequest: ProductSearchRequest): PaginatedResponse<ProductResponse> = query {
        val q = ProductTable.selectAll().andWhere { ProductTable.status eq ProductStatus.ACTIVE }
        if (searchRequest.name.isNotEmpty()) q.andWhere { ProductTable.name.like("%${searchRequest.name}%") }
        if (!searchRequest.categoryId.isNullOrEmpty()) q.andWhere { ProductTable.categoryId eq EntityID(searchRequest.categoryId, ProductCategoryTable) }
        searchRequest.minPrice?.let { q.andWhere { ProductTable.price greaterEq BigDecimal.valueOf(it) } }
        searchRequest.maxPrice?.let { q.andWhere { ProductTable.price lessEq BigDecimal.valueOf(it) } }
        q.toPaginatedResponse(searchRequest.limit, searchRequest.offset) { ProductDAO.wrapRow(it).toProductResponse() }
    }

    // ── Specialized lists ──────────────────────────────────────────────────

    override suspend fun getProductsByCategory(categoryId: String): PaginatedResponse<ProductResponse> = query {
        val condition: Op<Boolean> = (ProductTable.categoryId eq categoryId) and (ProductTable.status eq ProductStatus.ACTIVE)
        val data = ProductDAO.find(condition).map { it.toProductResponse() }
        PaginatedResponse(data, com.piashcse.utils.common.PaginationMetadata(ProductDAO.count(condition), data.size, 0))
    }

    override suspend fun getFeaturedProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:featured") {
            query {
                val condition: Op<Boolean> = (ProductTable.featured eq true) and (ProductTable.status eq ProductStatus.ACTIVE)
                val data = ProductDAO.find(condition).orderBy(ProductTable.createdAt to SortOrder.DESC).map { it.toProductResponse() }
                PaginatedResponse(data, PaginationMetadata(ProductDAO.count(condition), data.size, 0))
            }
        }

    override suspend fun getBestSellingProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:best-selling") {
            query {
                val condition: Op<Boolean> = (ProductTable.bestSeller eq true) and (ProductTable.status eq ProductStatus.ACTIVE)
                val data = ProductDAO.find(condition).orderBy(ProductTable.totalSales to SortOrder.DESC).limit(10).map { it.toProductResponse() }
                PaginatedResponse(data, PaginationMetadata(ProductDAO.count(condition), data.size, 0))
            }
        }

    override suspend fun getHotDealProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:hot-deals") {
            query {
                val condition: Op<Boolean> = (ProductTable.hotDeal eq true) and (ProductTable.status eq ProductStatus.ACTIVE)
                val data = ProductDAO.find(condition).orderBy(ProductTable.discountPercentage to SortOrder.DESC).limit(10).map { it.toProductResponse() }
                PaginatedResponse(data, PaginationMetadata(ProductDAO.count(condition), data.size, 0))
            }
        }
}
