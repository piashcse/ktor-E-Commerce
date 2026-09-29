package com.piashcse.feature.product_sub_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.feature.common.CatalogCrud
import com.piashcse.mapper.toProductSubCategoryResponse
import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class ProductSubCategoryRepositoryImpl : ProductSubCategoryRepository {
    override suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategoryResponse =
        query {
            ProductCategoryDAO.findById(productSubCategory.categoryId) ?: productSubCategory.categoryId.throwNotFound("Category")
            CatalogCrud.createUniqueByName(
                name = productSubCategory.name,
                validationLabel = null,
                conflictLabel = "Subcategory",
                findExisting = {
                    ProductSubCategoryDAO.find {
                        (ProductSubCategoryTable.categoryId eq productSubCategory.categoryId.entityID(ProductSubCategoryTable)) and
                            (ProductSubCategoryTable.name eq productSubCategory.name)
                    }.firstOrNull()
                },
                create = {
                    ProductSubCategoryDAO.new {
                        categoryId = productSubCategory.categoryId.entityID(ProductSubCategoryTable)
                        name = productSubCategory.name
                    }
                },
                toResponse = { it.toProductSubCategoryResponse() },
            )
        }

    override suspend fun getProductSubCategory(
        categoryId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductSubCategoryResponse> =
        query {
            ProductSubCategoryTable.selectAll().andWhere { ProductSubCategoryTable.categoryId eq categoryId }
                .toPaginatedResponse(limit, offset) {
                    ProductSubCategoryDAO.wrapRow(it).toProductSubCategoryResponse()
                }
        }

    override suspend fun updateProductSubCategory(
        id: String,
        name: String,
    ): ProductSubCategoryResponse =
        query {
            CatalogCrud.renameById(
                id = id,
                name = name,
                validationLabel = null,
                notFoundLabel = "Subcategory",
                findById = { ProductSubCategoryDAO.findById(it) },
                rename = { subCategory, newName -> subCategory.name = newName },
                toResponse = { it.toProductSubCategoryResponse() },
            )
        }

    override suspend fun deleteProductSubCategory(subCategoryId: String): String =
        query {
            CatalogCrud.deleteById(
                id = subCategoryId,
                notFoundLabel = "Subcategory",
                findById = { ProductSubCategoryDAO.findById(it) },
                delete = { it.delete() },
            )
        }
}
