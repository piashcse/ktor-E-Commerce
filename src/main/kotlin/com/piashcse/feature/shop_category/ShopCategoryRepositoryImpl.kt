package com.piashcse.feature.shop_category

import com.piashcse.database.entities.ShopCategoryDAO
import com.piashcse.database.entities.ShopCategoryTable
import com.piashcse.feature.common.CatalogCrud
import com.piashcse.mapper.toShopCategoryResponse
import com.piashcse.model.response.ShopCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopCategoryRepositoryImpl : ShopCategoryRepository {
    override suspend fun createCategory(name: String): ShopCategoryResponse =
        query {
            CatalogCrud.createUniqueByName(
                name = name,
                validationLabel = "ShopCategory",
                conflictLabel = "Category",
                findExisting = { ShopCategoryDAO.find { ShopCategoryTable.name eq name }.firstOrNull() },
                create = {
                    ShopCategoryDAO.new {
                        this.name = name
                    }
                },
                toResponse = { it.toShopCategoryResponse() },
            )
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
            CatalogCrud.renameById(
                id = categoryId,
                name = name,
                validationLabel = "ShopCategory",
                notFoundLabel = "Category",
                findById = { ShopCategoryDAO.findById(it) },
                rename = { category, newName -> category.name = newName },
                toResponse = { it.toShopCategoryResponse() },
            )
        }

    override suspend fun deleteCategory(categoryId: String): String =
        query {
            CatalogCrud.deleteById(
                id = categoryId,
                notFoundLabel = "Category",
                findById = { ShopCategoryDAO.findById(it) },
                delete = { it.delete() },
            )
        }
}
