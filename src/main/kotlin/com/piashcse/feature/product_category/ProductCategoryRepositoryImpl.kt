package com.piashcse.feature.product_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.mapper.toProductCategoryResponse
import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.toPaginatedList
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll

class ProductCategoryRepositoryImpl : ProductCategoryRepository,
    BaseCrudRepository<ProductCategoryDAO, ProductCategoryResponse>(ProductCategoryDAO, ProductCategoryTable, "Category") {

    override fun ProductCategoryDAO.toResponse(): ProductCategoryResponse = toProductCategoryResponse()

    override suspend fun createCategory(name: String): ProductCategoryResponse {
        if (exists { ProductCategoryTable.name eq name }) throw name.throwConflict("Category")
        return create { this.name = name }
    }

    override suspend fun getCategories(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductCategoryResponse> = query {
        val (totalCount, rows) = ProductCategoryTable.selectAll().toPaginatedList(limit, offset) {
            ProductCategoryDAO.wrapRow(it)
        }
        val categoryIds = rows.map { it.id }
        val subCategoriesMap = if (categoryIds.isNotEmpty()) {
            ProductSubCategoryDAO.find { ProductSubCategoryTable.categoryId inList categoryIds }
                .groupBy { it.categoryId.value }
        } else {
            emptyMap()
        }
        val data = rows.map { category ->
            category.toProductCategoryResponse(subCategoriesMap[category.id.value] ?: emptyList())
        }
        PaginatedResponse.of(data, totalCount, limit, offset)
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ProductCategoryResponse = update(categoryId) { this.name = name }

    override suspend fun deleteCategory(categoryId: String): String = delete(categoryId)
}
