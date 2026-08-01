package com.piashcse.feature.shop_category

import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery

class ShopCategoryService(private val shopCategoryRepo: ShopCategoryRepository) {
    /**
     * Creates a shop category. Runs in a retryable transaction.
     */
    suspend fun createCategory(name: String): ShopCategoryResponse = suspendRetryQuery { shopCategoryRepo.createCategory(name) }

    suspend fun getCategories(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ShopCategoryResponse> = shopCategoryRepo.getCategories(limit, offset)

    /**
     * Updates a shop category. Runs in a retryable transaction.
     */
    suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ShopCategoryResponse = suspendRetryQuery { shopCategoryRepo.updateCategory(categoryId, name) }

    /**
     * Deletes a shop category. Runs in a retryable transaction.
     */
    suspend fun deleteCategory(categoryId: String): String = suspendRetryQuery { shopCategoryRepo.deleteCategory(categoryId) }
}
