package com.piashcse.controller

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
    fun createProductSubCategory(productSubCategory: AddProductSubCategory) = transaction {
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

    fun getProductSubCategory(paging: PagingDataWithCategoryId) = transaction {
        val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.categoryId eq paging.categoryId }.limit(paging.limit, paging.offset)
        subCategoryExist.map {
            it.response()
        }
    }

    fun updateProductSubCategory(updateProductSubCategory: UpdateProductSubCategory) = transaction {
        val suCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq updateProductSubCategory.subCategoryId }.toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = updateProductSubCategory.subCategoryName
            it.response()
        } ?: run {
            updateProductSubCategory.subCategoryId.isNotExistException()
        }
    }

    fun deleteProductSubCategory(productSubCategoryId: String) = transaction {
        val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq productSubCategoryId }.toList().singleOrNull()
        subCategoryExist?.let {
            subCategoryExist.delete()
            productSubCategoryId
        }
    }
}