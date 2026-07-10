package com.piashcse.feature.product

import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse

class ProductQueryService(
    private val productRepo: ProductRepository,
) {
    suspend fun getProducts(productQuery: ProductWithFilterRequest): PaginatedResponse<ProductResponse> =
        productRepo.getProducts(productQuery)

    suspend fun getProductsByUser(
        userId: String,
        productQuery: ProductWithFilterRequest,
    ): PaginatedResponse<ProductResponse> =
        productRepo.getProductsByUser(userId, productQuery)

    suspend fun getProductsByShop(
        shopId: String,
        productQuery: ProductWithFilterRequest,
    ): PaginatedResponse<ProductResponse> =
        productRepo.getProductsByShop(shopId, productQuery)

    suspend fun getProductsByCategory(categoryId: String): PaginatedResponse<ProductResponse> =
        productRepo.getProductsByCategory(categoryId)

    suspend fun searchProduct(productQuery: ProductSearchRequest): PaginatedResponse<ProductResponse> =
        productRepo.searchProduct(productQuery)
}
