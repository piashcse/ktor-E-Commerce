package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery

class ProductSubCategoryService(private val subCategoryRepo: ProductSubCategoryRepository) {
    /**
     * Adds a product subcategory. Runs in a retryable transaction.
     */
    suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategoryResponse =
        suspendRetryQuery { subCategoryRepo.addProductSubCategory(productSubCategory) }

    suspend fun getProductSubCategory(
        categoryId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ProductSubCategoryResponse> = subCategoryRepo.getProductSubCategory(categoryId, limit, offset)

    /**
     * Updates a product subcategory. Runs in a retryable transaction.
     */
    suspend fun updateProductSubCategory(
        id: String,
        name: String,
    ): ProductSubCategoryResponse = suspendRetryQuery { subCategoryRepo.updateProductSubCategory(id, name) }

    /**
     * Deletes a product subcategory. Runs in a retryable transaction.
     */
    suspend fun deleteProductSubCategory(subCategoryId: String): String =
        suspendRetryQuery { subCategoryRepo.deleteProductSubCategory(subCategoryId) }
}
