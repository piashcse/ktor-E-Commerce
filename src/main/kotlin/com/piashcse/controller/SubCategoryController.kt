package com.piashcse.controller

import com.piashcse.entities.category.CategoryEntity
import com.piashcse.entities.category.CategoryTable
import com.piashcse.entities.category.SubCategoryEntity
import com.piashcse.entities.category.SubCategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.subcategory.AddSubCategory
import com.piashcse.models.subcategory.UpdateSubCategory
import com.piashcse.utils.CommonException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class SubCategoryController {
    fun createSubCategory(subCategory: AddSubCategory) = transaction {
        val categoryIdExist = CategoryEntity.find { CategoryTable.id eq subCategory.categoryId }.toList().singleOrNull()
        return@transaction if (categoryIdExist != null) {
            val subCategoryExist =
                SubCategoryEntity.find { SubCategoryTable.subCategoryName eq subCategory.subCategoryName }.toList()
                    .singleOrNull()
            if (subCategoryExist == null) {
                SubCategoryEntity.new {
                    categoryId = EntityID(subCategory.categoryId, SubCategoryTable)
                    subCategoryName = subCategory.subCategoryName
                }.subCategoryResponse()
            } else {
                throw CommonException("SubCategory name ${subCategory.subCategoryName} already exist")
            }
        } else {
            throw CommonException("CategoryId ${subCategory.categoryId} not exist")
        }
    }

    fun getSubCategory(paging: PagingData) = transaction {
        val subCategoryExist = SubCategoryEntity.all().limit(paging.limit, paging.offset)
        return@transaction subCategoryExist.map {
            it.subCategoryResponse()
        }
    }

    fun updateSubCategory(updateSubCategory: UpdateSubCategory) = transaction {
        val suCategoryExist =
            SubCategoryEntity.find { SubCategoryTable.id eq updateSubCategory.subCategoryId }.toList().singleOrNull()
        suCategoryExist?.let {
            it.subCategoryName = updateSubCategory.subCategoryName
        } ?: run {
            throw CommonException("Category not  exist")
        }
    }

    fun deleteSubCategory(subCategoryId: String) = transaction {
        val subCategoryExist = SubCategoryEntity.find { SubCategoryTable.id eq subCategoryId }.toList().singleOrNull()
        subCategoryExist?.delete()
    }
}