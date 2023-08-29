package com.piashcse.controller

import com.piashcse.entities.product.category.ProductCategoryEntity
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.entities.product.category.ProductSubCategoryEntity
import com.piashcse.entities.product.category.ProductSubCategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.subcategory.AddSubCategory
import com.piashcse.models.subcategory.UpdateSubCategory
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class ProductSubCategoryController {
    fun createSubCategory(subCategory: AddSubCategory) = transaction {
        val categoryIdExist = ProductCategoryEntity.find { ProductCategoryTable.id eq subCategory.categoryId }.toList().singleOrNull()
        if (categoryIdExist != null) {
            val subCategoryExist =
                ProductSubCategoryEntity.find { ProductSubCategoryTable.subCategoryName eq subCategory.subCategoryName }.toList()
                    .singleOrNull()
            if (subCategoryExist == null) {
                ProductSubCategoryEntity.new {
                    categoryId = EntityID(subCategory.categoryId, ProductSubCategoryTable)
                    subCategoryName = subCategory.subCategoryName
                }.response()
            } else {
                subCategory.subCategoryName.alreadyExistException()
            }
        } else {
            subCategory.categoryId.isNotExistException()
        }
    }

    fun getSubCategory(paging: PagingData) = transaction {
        val subCategoryExist = ProductSubCategoryEntity.all().limit(paging.limit, paging.offset)
        subCategoryExist.map {
            it.response()
        }
    }

    fun updateSubCategory(updateSubCategory: UpdateSubCategory) = transaction {
        val suCategoryExist =
            ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq updateSubCategory.subCategoryId }.toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = updateSubCategory.subCategoryName
        } ?: run {
            updateSubCategory.subCategoryId.isNotExistException()
        }
    }

    fun deleteSubCategory(subCategoryId: String) = transaction {
        val subCategoryExist = ProductSubCategoryEntity.find { ProductSubCategoryTable.id eq subCategoryId }.toList().singleOrNull()
        subCategoryExist?.delete()
    }
}