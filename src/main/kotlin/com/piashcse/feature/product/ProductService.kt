package com.piashcse.feature.product

import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.common.PaginatedResponse

class ProductService(
    private val productRepo: ProductRepository,
    private val cache: Cache = CacheService.cache,
) : ProductRepository by productRepo {

    private suspend fun <T> cachedOrQuery(cacheKey: String, query: suspend () -> T): T {
        cache.get<T>(cacheKey)?.let { return it }
        return query().also { cache.set(cacheKey, it) }
    }

    override suspend fun createProduct(
        userId: String,
        shopId: String?,
        productRequest: ProductRequest,
    ): ProductResponse =
        productRepo.createProduct(userId, shopId, productRequest)
            .also { cache.invalidatePattern("products:.*") }

    override suspend fun updateProduct(
        userId: String,
        productId: String,
        updateProduct: UpdateProductRequest,
    ): ProductResponse =
        productRepo.updateProduct(userId, productId, updateProduct)
            .also { cache.invalidatePattern("products:.*") }

    override suspend fun deleteProduct(userId: String, productId: String): String =
        productRepo.deleteProduct(userId, productId)
            .also { cache.invalidatePattern("products:.*") }

    override suspend fun getProductDetail(productId: String): ProductResponse =
        cachedOrQuery("products:detail:$productId") { productRepo.getProductDetail(productId) }

    override suspend fun getFeaturedProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:featured") { productRepo.getFeaturedProducts() }

    override suspend fun getBestSellingProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:best-selling") { productRepo.getBestSellingProducts() }

    override suspend fun getHotDealProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:hot-deals") { productRepo.getHotDealProducts() }

    override suspend fun deleteProductAsAdmin(productId: String): String =
        productRepo.deleteProductAsAdmin(productId)
            .also { cache.invalidatePattern("products:.*") }
}
