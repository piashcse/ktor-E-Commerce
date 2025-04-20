package com.piashcse.modules.productcategory.repository

import com.piashcse.database.entities.ProductCategory

interface ProductCategoryRepo {
    /**
     * Creates a new product category.
     *
     * @param name The name of the category.
     * @return The created product category.
     */
    suspend fun createCategory(name: String): ProductCategory

    /**
     * Retrieves a list of product categories.
     *
     * @param limit The maximum number of categories to return.
     * @return A list of product categories.
     */
    suspend fun getCategories(limit: Int): List<ProductCategory>

    /**
     * Updates an existing product category.
     *
     * @param categoryId The unique identifier of the category.
     * @param name The updated category name.
     * @return The updated product category.
     */
    suspend fun updateCategory(categoryId: String, name: String): ProductCategory

    /**
     * Deletes a product category.
     *
     * @param categoryId The unique identifier of the category.
     * @return A confirmation message.
     */
    suspend fun deleteCategory(categoryId: String): String
}