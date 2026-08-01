package com.piashcse.feature.product_category

import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery

class ProductCategoryService(private val productCategoryRepo: ProductCategoryRepository) {
    /**
     * Creates a product category. Runs in a retryable transaction.
     */
    suspend fun createCategory(name: String): ProductCategoryResponse = suspendRetryQuery { productCategoryRepo.createCategory(name) }

    suspend fun getCategories(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ProductCategoryResponse> = productCategoryRepo.getCategories(limit, offset)

    /**
     * Updates a product category. Runs in a retryable transaction.
     */
    suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ProductCategoryResponse = suspendRetryQuery { productCategoryRepo.updateCategory(categoryId, name) }

    /**
     * Deletes a product category. Runs in a retryable transaction.
     */
    suspend fun deleteCategory(categoryId: String): String = suspendRetryQuery { productCategoryRepo.deleteCategory(categoryId) }
}
