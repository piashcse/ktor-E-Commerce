package com.piashcse.feature.shop_category

import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.mapper.toShopCategoryResponse
import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopCategoryRepositoryImpl : ShopCategoryRepository {
    override suspend fun createCategory(name: String): ShopCategoryResponse =
        query {
            val isExistShopCategory =
                ShopCategoryDAO.find { ShopCategoryTable.name eq name }.firstOrNull()
            isExistShopCategory?.let {
                throw name.throwConflict("Category")
            } ?: ShopCategoryDAO.new {
                this.name = name
            }.toShopCategoryResponse()
        }

    override suspend fun getCategories(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ShopCategoryResponse> =
        query {
            ShopCategoryTable.selectAll().toPaginatedResponse(limit, offset) {
                ShopCategoryDAO.wrapRow(it).toShopCategoryResponse()
            }
        }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ShopCategoryResponse =
        query {
            val isShopCategoryExist =
                ShopCategoryDAO.findById(categoryId)
            isShopCategoryExist?.let {
                it.name = name
                it.toShopCategoryResponse()
            } ?: categoryId.throwNotFound("Category")
        }

    override suspend fun deleteCategory(categoryId: String): String =
        query {
            val shopCategoryExist =
                ShopCategoryDAO.findById(categoryId)
            shopCategoryExist?.let {
                it.delete()
                categoryId
            } ?: categoryId.throwNotFound("Category")
        }
}
