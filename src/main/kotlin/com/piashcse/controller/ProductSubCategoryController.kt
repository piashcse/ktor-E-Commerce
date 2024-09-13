package com.piashcse.controller

import com.piashcse.entities.product.category.*
import com.piashcse.models.subcategory.AddProductSubCategory
import com.piashcse.repository.ProductSubCategoryRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID

class ProductSubCategoryController : ProductSubCategoryRepo {
    override suspend fun addProductSubCategory(productSubCategory: AddProductSubCategory): ProductSubCategory = query {
        val isCategoryIdExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq productSubCategory.categoryId }.toList()
                .singleOrNull()
        isCategoryIdExist?.let {
            val isSubCategoryExist =
                ProductSubCategoryEntity.find { ProductSubCategoryTable.subCategoryName eq productSubCategory.subCategoryName }
                    .toList()
                    .singleOrNull()
            isSubCategoryExist?.let {
                throw productSubCategory.subCategoryName.alreadyExistException()
            } ?: ProductSubCategoryEntity.new {
                categoryId = EntityID(productSubCategory.categoryId, ProductSubCategoryTable)
                subCategoryName = productSubCategory.subCategoryName
            }.response()
        } ?: throw productSubCategory.categoryId.notFoundException()
    }

    override suspend fun getProductSubCategory(categoryId: String, limit: Int, offset: Long): List<ProductSubCategory> =
        query {
            val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.categoryId eq categoryId }
                .limit(limit, offset)
            subCategoryExist.map {
                it.response()
            }
        }

    override suspend fun updateProductSubCategory(id: String, subCategoryName: String): ProductSubCategory = query {
        val suCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq id }
                .toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = subCategoryName
            it.response()
        } ?: throw id.notFoundException()

    }

    override suspend fun deleteProductSubCategory(subCategoryId: String): String = query {
        val isSubCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq subCategoryId }.toList().singleOrNull()
        isSubCategoryExist?.let {
            isSubCategoryExist.delete()
            subCategoryId
        } ?: throw subCategoryId.notFoundException()
    }
}