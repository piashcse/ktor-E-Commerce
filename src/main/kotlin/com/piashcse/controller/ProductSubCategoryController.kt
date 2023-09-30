package com.piashcse.controller

import com.piashcse.dbhelper.query
import com.piashcse.entities.product.category.ProductCategoryEntity
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.entities.product.category.ProductSubCategoryEntity
import com.piashcse.entities.product.category.ProductSubCategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.subcategory.AddProductSubCategory
import com.piashcse.models.subcategory.PagingDataWithCategoryId
import com.piashcse.models.subcategory.UpdateProductSubCategory
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class ProductSubCategoryController {
    suspend fun createProductSubCategory(productSubCategory: AddProductSubCategory) = query {
        val categoryIdExist = ProductCategoryEntity.find { ProductCategoryTable.id eq productSubCategory.categoryId }.toList().singleOrNull()
        if (categoryIdExist != null) {
            val subCategoryExist =
                ProductSubCategoryEntity.find { ProductSubCategoryTable.subCategoryName eq productSubCategory.subCategoryName }.toList()
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

    suspend fun getProductSubCategory(paging: PagingDataWithCategoryId) = query {
        val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.categoryId eq paging.categoryId }.limit(paging.limit, paging.offset)
        subCategoryExist.map {
            it.response()
        }
    }

    suspend fun updateProductSubCategory(updateProductSubCategory: UpdateProductSubCategory) = query {
        val suCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq updateProductSubCategory.subCategoryId }.toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = updateProductSubCategory.subCategoryName
            it.response()
        } ?: run {
            updateProductSubCategory.subCategoryId.isNotExistException()
        }
    }

    suspend fun deleteProductSubCategory(productSubCategoryId: String) = query {
        val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq productSubCategoryId }.toList().singleOrNull()
        subCategoryExist?.let {
            subCategoryExist.delete()
            productSubCategoryId
        }
    }
}