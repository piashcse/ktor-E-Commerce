package com.piashcse.feature.product

import com.piashcse.constants.CacheKeys
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.SearchResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.common.PaginatedResponse

class ProductCatalogService(
    private val productRepo: ProductRepository,
    private val cache: Cache = CacheService.cache,
) {
    private suspend fun <T> cachedOrQuery(cacheKey: String, query: suspend () -> T): T {
        cache.get<T>(cacheKey)?.let { return it }
        return query().also { cache.set(cacheKey, it) }
    }

    suspend fun getProductDetail(productId: String): ProductResponse =
        cachedOrQuery(CacheKeys.Products.detail(productId)) { productRepo.getProductDetail(productId) }

    suspend fun getProducts(productQuery: ProductWithFilterRequest): PaginatedResponse<ProductResponse> =
        productRepo.getProducts(productQuery)

    suspend fun getProductsByUser(
        userId: String,
        productQuery: ProductWithFilterRequest,
    ): PaginatedResponse<ProductResponse> = productRepo.getProductsByUser(userId, productQuery)

    suspend fun incrementViewCount(productId: String) {
        productRepo.incrementViewCount(productId)
        cache.invalidatePattern(CacheKeys.Products.detail(productId))
    }

    suspend fun getFeaturedProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery(CacheKeys.Products.FEATURED) { productRepo.getFeaturedProducts() }

    suspend fun getBestSellingProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery(CacheKeys.Products.BEST_SELLING) { productRepo.getBestSellingProducts() }

    suspend fun getHotDealProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery(CacheKeys.Products.HOT_DEALS) { productRepo.getHotDealProducts() }

    suspend fun searchProduct(productQuery: ProductSearchRequest): SearchResponse =
        productRepo.searchProduct(productQuery)
}
