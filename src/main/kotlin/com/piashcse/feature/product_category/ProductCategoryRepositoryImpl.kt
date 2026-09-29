package com.piashcse.feature.product_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductCategoryTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.database.entities.ProductTable
import com.piashcse.feature.common.CatalogCrud
import com.piashcse.mapper.toProductCategoryResponse
import com.piashcse.model.response.ProductCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.paginateWithPreload
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ConflictException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll

class ProductCategoryRepositoryImpl : ProductCategoryRepository {
    override suspend fun createCategory(name: String): ProductCategoryResponse =
        query {
            CatalogCrud.createUniqueByName(
                name = name,
                validationLabel = "ProductCategory",
                conflictLabel = "Category",
                findExisting = { ProductCategoryDAO.find { ProductCategoryTable.name eq name }.firstOrNull() },
                create = {
                    ProductCategoryDAO.new {
                        this.name = name
                    }
                },
                toResponse = { it.toProductCategoryResponse() },
            )
        }

    override suspend fun getCategories(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductCategoryResponse> =
        query {
            ProductCategoryTable.selectAll().paginateWithPreload(
                limit,
                offset,
                rowMapper = { ProductCategoryDAO.wrapRow(it) },
            ) { rows ->
                val categoryIds = rows.map { it.id }
                val subCategoriesMap =
                    if (categoryIds.isNotEmpty()) {
                        ProductSubCategoryDAO.find { ProductSubCategoryTable.categoryId inList categoryIds }
                            .groupBy { it.categoryId.value }
                    } else {
                        emptyMap()
                    }
                rows.map { category ->
                    category.toProductCategoryResponse(subCategoriesMap[category.id.value] ?: emptyList())
                }
            }
        }

    override suspend fun updateCategory(
        categoryId: String,
        name: String,
    ): ProductCategoryResponse =
        query {
            CatalogCrud.renameById(
                id = categoryId,
                name = name,
                validationLabel = "ProductCategory",
                notFoundLabel = "Category",
                findById = { ProductCategoryDAO.findById(it) },
                rename = { category, newName -> category.name = newName },
                toResponse = { it.toProductCategoryResponse() },
            )
        }

    override suspend fun deleteCategory(categoryId: String): String =
        query {
            CatalogCrud.deleteById(
                id = categoryId,
                notFoundLabel = "Category",
                findById = { ProductCategoryDAO.findById(it) },
                guard = {
                    if (!ProductSubCategoryDAO.find { ProductSubCategoryTable.categoryId eq categoryId }.empty()) {
                        throw ConflictException("Cannot delete category: sub-categories still reference it.")
                    }
                    if (!ProductDAO.find { ProductTable.categoryId eq categoryId }.empty()) {
                        throw ConflictException(
                            "Cannot delete category: products still reference it. Reassign or soft-delete products first.",
                        )
                    }
                },
                delete = { it.delete() },
            )
        }
}
