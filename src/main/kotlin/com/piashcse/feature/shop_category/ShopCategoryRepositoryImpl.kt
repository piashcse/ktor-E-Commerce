package com.piashcse.feature.shop_category

import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.mapper.toShopCategoryResponse
import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.throwConflict
import org.jetbrains.exposed.v1.core.eq

class ShopCategoryRepositoryImpl : ShopCategoryRepository,
    BaseCrudRepository<ShopCategoryDAO, ShopCategoryResponse>(ShopCategoryDAO, ShopCategoryTable, "Category") {

    override fun ShopCategoryDAO.toResponse(): ShopCategoryResponse = toShopCategoryResponse()

    override suspend fun createCategory(name: String): ShopCategoryResponse {
        if (exists { ShopCategoryTable.name eq name }) throw name.throwConflict("Category")
        return create { this.name = name }
    }

    override suspend fun getCategories(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopCategoryResponse> = getAll(limit, offset)

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ShopCategoryResponse = update(categoryId) { this.name = name }

    override suspend fun deleteCategory(categoryId: String): String = delete(categoryId)
}
