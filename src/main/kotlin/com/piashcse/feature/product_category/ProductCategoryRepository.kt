package com.piashcse.feature.product_category

import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.utils.common.PaginatedResponse

interface ProductCategoryRepository {
    /**
     * Creates a new product category.
     *
     * @param name The name of the category.
     * @return The created product category.
     */
    suspend fun createCategory(name: String): ProductCategoryResponse

    /**
     * Retrieves a list of product categories.
     *
     * @param limit The maximum number of categories to return.
     * @return A list of product categories.
     */
    suspend fun getCategories(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ProductCategoryResponse>

    /**
     * Updates an existing product category.
     *
     * @param categoryId The unique identifier of the category.
     * @param name The updated category name.
     * @return The updated product category.
     */
    suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ProductCategoryResponse

    /**
     * Deletes a product category.
     *
     * @param categoryId The unique identifier of the category.
     * @return A confirmation message.
     */
    suspend fun deleteCategory(categoryId: String): String
}
