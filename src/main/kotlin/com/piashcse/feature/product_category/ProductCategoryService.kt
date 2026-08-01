package com.piashcse.feature.product_category

import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.utils.common.PaginatedResponse

class ProductCategoryService(private val productCategoryRepo: ProductCategoryRepository) {
    suspend fun createCategory(name: String): ProductCategoryResponse = productCategoryRepo.createCategory(name)

    suspend fun getCategories(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ProductCategoryResponse> = productCategoryRepo.getCategories(limit, offset)

    suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ProductCategoryResponse = productCategoryRepo.updateCategory(categoryId, name)

    suspend fun deleteCategory(categoryId: String): String = productCategoryRepo.deleteCategory(categoryId)
}
