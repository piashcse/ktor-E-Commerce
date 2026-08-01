package com.piashcse.feature.shop_category

import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.utils.common.PaginatedResponse

class ShopCategoryService(private val shopCategoryRepo: ShopCategoryRepository) {
    suspend fun createCategory(name: String): ShopCategoryResponse = shopCategoryRepo.createCategory(name)

    suspend fun getCategories(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ShopCategoryResponse> = shopCategoryRepo.getCategories(limit, offset)

    suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ShopCategoryResponse = shopCategoryRepo.updateCategory(categoryId, name)

    suspend fun deleteCategory(categoryId: String): String = shopCategoryRepo.deleteCategory(categoryId)
}
