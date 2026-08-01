package com.piashcse.feature.product_sub_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.mapper.toProductSubCategoryResponse
import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

class ProductSubCategoryRepositoryImpl : ProductSubCategoryRepository,
    BaseCrudRepository<ProductSubCategoryDAO, ProductSubCategoryResponse>(
        ProductSubCategoryDAO,
        ProductSubCategoryTable,
        "Subcategory",
    ) {

    override fun ProductSubCategoryDAO.toResponse(): ProductSubCategoryResponse = toProductSubCategoryResponse()

    override suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategoryResponse {
        ProductCategoryDAO.findById(productSubCategory.categoryId) ?: productSubCategory.categoryId.throwNotFound("Category")
        val exists = exists {
            (ProductSubCategoryTable.categoryId eq productSubCategory.categoryId.entityID(ProductSubCategoryTable)) and
                (ProductSubCategoryTable.name eq productSubCategory.name)
        }
        if (exists) throw productSubCategory.name.throwConflict("Subcategory")
        return create {
            categoryId = productSubCategory.categoryId.entityID(ProductSubCategoryTable)
            name = productSubCategory.name
        }
    }

    override suspend fun getProductSubCategory(
        categoryId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductSubCategoryResponse> =
        getAll(limit, offset) { ProductSubCategoryTable.categoryId eq categoryId }

    override suspend fun updateProductSubCategory(
        id: String,
        name: String,
    ): ProductSubCategoryResponse = update(id) { this.name = name }

    override suspend fun deleteProductSubCategory(subCategoryId: String): String = delete(subCategoryId)
}
