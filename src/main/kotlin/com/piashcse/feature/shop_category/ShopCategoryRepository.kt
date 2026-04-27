package com.piashcse.feature.shop_category

import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.utils.common.PaginatedResponse

interface ShopCategoryRepository {
    /**
     * Creates a new shop category.
     *
     * @param categoryName The name of the shop category.
     * @return The created shop category.
     */
    suspend fun createCategory(name: String): ShopCategoryResponse

    /**
     * Retrieves a list of shop categories with a limit.
     *
     * @param limit The maximum number of categories to return.
     * @return A list of shop categories.
     */
    suspend fun getCategories(limit: Int, offset: Int = 0): PaginatedResponse<ShopCategoryResponse>

    /**
     * Updates an existing shop category.
     *
     * @param categoryId The unique identifier of the shop category.
     * @param categoryName The updated name of the shop category.
     * @return The updated shop category.
     */
    suspend fun updateCategory(categoryId: String, name: String): ShopCategoryResponse

    /**
     * Deletes a specific shop category.
     *
     * @param categoryId The unique identifier of the shop category to delete.
     * @return A confirmation message.
     */
    suspend fun deleteCategory(categoryId: String): String
}