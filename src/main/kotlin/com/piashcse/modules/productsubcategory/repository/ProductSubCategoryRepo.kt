package com.piashcse.modules.productsubcategory.repository

import com.piashcse.database.entities.ProductSubCategory
import com.piashcse.database.models.subcategory.ProductSubCategoryRequest

/**
 * Repository interface for managing product subcategories.
 * Provides methods to add, retrieve, update, and delete product subcategories.
 */
interface ProductSubCategoryRepo {

    /**
     * Adds a new product subcategory.
     *
     * @param productSubCategory The request object containing details of the subcategory to be added.
     * @return The newly created product subcategory.
     */
    suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategory

    /**
     * Retrieves a list of product subcategories for a given category.
     *
     * @param categoryId The ID of the parent category.
     * @param limit The maximum number of subcategories to retrieve.
     * @return A list of product subcategories.
     */
    suspend fun getProductSubCategory(categoryId: String, limit: Int): List<ProductSubCategory>

    /**
     * Updates an existing product subcategory.
     *
     * @param id The ID of the subcategory to update.
     * @param name The new name for the subcategory.
     * @return The updated product subcategory.
     */
    suspend fun updateProductSubCategory(id: String, name: String): ProductSubCategory

    /**
     * Deletes a product subcategory.
     *
     * @param subCategoryId The ID of the subcategory to delete.
     * @return The ID of the deleted subcategory.
     */
    suspend fun deleteProductSubCategory(subCategoryId: String): String
}