package com.piashcse.feature.product

import com.piashcse.constants.CacheKeys
import com.piashcse.constants.Message
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException

class ProductCrudService(
    private val productRepo: ProductRepository,
    private val cache: Cache = CacheService.cache,
) {
    private suspend fun <T> withCacheInvalidation(block: suspend () -> T): T =
        suspendRetryQuery { block() }.also { cache.invalidatePattern(CacheKeys.PRODUCTS_PATTERN) }

    /**
     * Creates a product after verifying the caller is a seller and owns the
     * target shop, then invalidates the product cache. Runs in a retryable
     * transaction.
     */
    suspend fun createProduct(
        userId: String,
        shopId: String?,
        productRequest: ProductRequest,
    ): ProductResponse =
        withCacheInvalidation {
            val access = productRepo.getCreateProductAccess(userId, shopId)
            if (!access.sellerExists) throw NotFoundException(Message.Errors.SELLER_REQUIRED)
            if (access.shopOwnerUserId != null && access.shopOwnerUserId != userId)
                throw ForbiddenException(Message.Products.NOT_SHOP_OWNER)
            productRepo.createProduct(userId, access.resolvedShopId, productRequest)
        }

    /**
     * Updates a product after verifying ownership and invalidates the product
     * cache. Runs in a retryable transaction.
     */
    suspend fun updateProduct(
        userId: String,
        productId: String,
        updateProduct: UpdateProductRequest,
    ): ProductResponse =
        withCacheInvalidation {
            val access = productRepo.getProductAccess(userId, productId)
            if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("product"))
            productRepo.updateProduct(productId, updateProduct)
        }

    /**
     * Deletes a product after verifying ownership and invalidates the product
     * cache. Runs in a retryable transaction.
     */
    suspend fun deleteProduct(userId: String, productId: String): String =
        withCacheInvalidation {
            val access = productRepo.getProductAccess(userId, productId)
            if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("product"))
            productRepo.deleteProduct(productId)
        }

    /**
     * Deletes a product as admin and invalidates the product cache. Runs in a
     * retryable transaction.
     */
    suspend fun deleteProductAsAdmin(productId: String): String =
        withCacheInvalidation { productRepo.deleteProductAsAdmin(productId) }
}
