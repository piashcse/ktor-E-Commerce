package com.piashcse.feature.product_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.mapper.toProductCategoryResponse
import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class ProductCategoryRepositoryImpl : ProductCategoryRepository {
    override suspend fun createCategory(name: String): ProductCategoryResponse =
        query {
            val isCategoryExist =
                ProductCategoryDAO.find { ProductCategoryTable.name eq name }.firstOrNull()
            isCategoryExist?.let {
                throw name.throwConflict("Category")
            } ?: ProductCategoryDAO.new {
                this.name = name
            }.toProductCategoryResponse()
        }

    override suspend fun getCategories(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductCategoryResponse> =
        query {
            ProductCategoryTable.selectAll().toPaginatedResponse(limit, offset) {
                ProductCategoryDAO.wrapRow(it).toProductCategoryResponse()
            }
        }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ProductCategoryResponse =
        query {
            val isCategoryExist =
                ProductCategoryDAO.findById(categoryId)
            isCategoryExist?.let {
                it.name = name
                it.toProductCategoryResponse()
            } ?: categoryId.throwNotFound("Category")
        }

    override suspend fun deleteCategory(categoryId: String): String =
        query {
            val isCategoryExist =
                ProductCategoryDAO.findById(categoryId)
            isCategoryExist?.let {
                isCategoryExist.delete()
                categoryId
            } ?: categoryId.throwNotFound("Category")
        }
}
