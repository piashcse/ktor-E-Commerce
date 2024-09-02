package com.piashcse.controller

import com.piashcse.entities.product.category.ProductCategoryEntity
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.entities.product.category.ProductSubCategoryEntity
import com.piashcse.entities.product.category.ProductSubCategoryTable
import com.piashcse.models.subcategory.AddProductSubCategory
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID

class ProductSubCategoryController {
    suspend fun addProductSubCategory(productSubCategory: AddProductSubCategory) = query {
        val categoryIdExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq productSubCategory.categoryId }.toList()
                .singleOrNull()
        if (categoryIdExist != null) {
            val subCategoryExist =
                ProductSubCategoryEntity.find { ProductSubCategoryTable.subCategoryName eq productSubCategory.subCategoryName }
                    .toList()
                    .singleOrNull()
            if (subCategoryExist == null) {
                ProductSubCategoryEntity.new {
                    categoryId = EntityID(productSubCategory.categoryId, ProductSubCategoryTable)
                    subCategoryName = productSubCategory.subCategoryName
                }.response()
            } else {
                productSubCategory.subCategoryName.alreadyExistException()
            }
        } else {
            productSubCategory.categoryId.isNotExistException()
        }
    }

    suspend fun getProductSubCategory(categoryId: String, limit: Int, offset: Long) = query {
        val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.categoryId eq categoryId }
            .limit(limit, offset)
        subCategoryExist.map {
            it.response()
        }
    }

    suspend fun updateProductSubCategory(id: String, subCategoryName: String) = query {
        val suCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq id }
                .toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = subCategoryName
            it.response()
        } ?: run {
            id.isNotExistException()
        }
    }

    suspend fun deleteProductSubCategory(subCategoryId: String) = query {
        val subCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq subCategoryId }.toList().singleOrNull()
        subCategoryExist?.let {
            subCategoryExist.delete()
            subCategoryId
        }
    }
}