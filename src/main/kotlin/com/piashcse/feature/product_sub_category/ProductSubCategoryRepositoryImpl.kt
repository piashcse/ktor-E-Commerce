package com.piashcse.feature.product_sub_category

import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryDAO
import com.piashcse.database.entities.ProductSubCategoryTable
import com.piashcse.mapper.toProductSubCategoryResponse
import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.model.response.ProductSubCategoryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class ProductSubCategoryRepositoryImpl : ProductSubCategoryRepository {
    override suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategoryResponse =
        query {
            ProductCategoryDAO.findById(productSubCategory.categoryId) ?: productSubCategory.categoryId.throwNotFound("Category")
            val isSubCategoryExist = ProductSubCategoryDAO.find {
                (ProductSubCategoryTable.categoryId eq EntityID(productSubCategory.categoryId, ProductSubCategoryTable)) and
                    (ProductSubCategoryTable.name eq productSubCategory.name)
            }.firstOrNull()
            isSubCategoryExist?.let { throw productSubCategory.name.throwConflict("Subcategory") }
            ProductSubCategoryDAO.new {
                categoryId = EntityID(productSubCategory.categoryId, ProductSubCategoryTable)
                name = productSubCategory.name
            }.toProductSubCategoryResponse()
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
            val subCategory = ProductSubCategoryDAO.findById(id) ?: id.throwNotFound("Subcategory")
            subCategory.name = name
            subCategory.toProductSubCategoryResponse()
        }

    override suspend fun deleteProductSubCategory(subCategoryId: String): String =
        query {
            val subCategory = ProductSubCategoryDAO.findById(subCategoryId) ?: subCategoryId.throwNotFound("Subcategory")
            subCategory.delete()
            subCategoryId
        }
}
