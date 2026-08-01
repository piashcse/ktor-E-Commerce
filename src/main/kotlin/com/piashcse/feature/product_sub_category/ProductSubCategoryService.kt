package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.utils.common.PaginatedResponse

class ProductSubCategoryService(private val subCategoryRepo: ProductSubCategoryRepository) {
    suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategoryResponse =
        subCategoryRepo.addProductSubCategory(productSubCategory)

    suspend fun getProductSubCategory(
        categoryId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ProductSubCategoryResponse> = subCategoryRepo.getProductSubCategory(categoryId, limit, offset)

    suspend fun updateProductSubCategory(
        id: String,
        name: String,
    ): ProductSubCategoryResponse = subCategoryRepo.updateProductSubCategory(id, name)

    suspend fun deleteProductSubCategory(subCategoryId: String): String = subCategoryRepo.deleteProductSubCategory(subCategoryId)
}
