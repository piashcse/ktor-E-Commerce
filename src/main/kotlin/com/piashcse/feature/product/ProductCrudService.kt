package com.piashcse.feature.product

import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService

class ProductCrudService(
    private val productRepo: ProductRepository,
    private val cache: Cache = CacheService.cache,
) {
    suspend fun createProduct(
        userId: String,
        shopId: String?,
        productRequest: ProductRequest,
    ): ProductResponse =
        productRepo.createProduct(userId, shopId, productRequest)
            .also { cache.invalidatePattern("products:.*") }

    suspend fun updateProduct(
        userId: String,
        productId: String,
        updateProduct: UpdateProductRequest,
    ): ProductResponse =
        productRepo.updateProduct(userId, productId, updateProduct)
            .also { cache.invalidatePattern("products:.*") }

    suspend fun deleteProduct(userId: String, productId: String): String =
        productRepo.deleteProduct(userId, productId)
            .also { cache.invalidatePattern("products:.*") }

    suspend fun deleteProductAsAdmin(productId: String): String =
        productRepo.deleteProductAsAdmin(productId)
            .also { cache.invalidatePattern("products:.*") }
}
