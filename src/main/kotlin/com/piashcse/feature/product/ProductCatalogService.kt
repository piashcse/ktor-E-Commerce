package com.piashcse.feature.product

import com.piashcse.model.request.ProductSearchRequest
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
        cachedOrQuery("products:detail:$productId") { productRepo.getProductDetail(productId) }

    suspend fun getFeaturedProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:featured") { productRepo.getFeaturedProducts() }

    suspend fun getBestSellingProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:best-selling") { productRepo.getBestSellingProducts() }

    suspend fun getHotDealProducts(): PaginatedResponse<ProductResponse> =
        cachedOrQuery("products:hot-deals") { productRepo.getHotDealProducts() }

    suspend fun searchProduct(productQuery: ProductSearchRequest): SearchResponse =
        productRepo.searchProduct(productQuery)
}
