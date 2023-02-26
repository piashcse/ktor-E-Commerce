package com.example.controller

import com.example.entities.category.CategoryEntity
import com.example.entities.category.CategoryTable
import com.example.models.PagingData
import com.example.models.category.AddCategory
import com.example.models.category.DeleteCategory
import com.example.models.category.UpdateCategory
import com.example.utils.CommonException
import org.jetbrains.exposed.sql.transactions.transaction

class CategoryController {
    fun createCategory(addCategory: AddCategory) = transaction {
        val categoryExist =
            CategoryEntity.find { CategoryTable.categoryName eq addCategory.categoryName }.toList().singleOrNull()
        return@transaction if (categoryExist == null) {
            CategoryEntity.new {
                categoryName = addCategory.categoryName
            }.categoryResponse()
        } else {
            throw CommonException("${addCategory.categoryName} already exist")
        }
    }

    fun getCategory(paging: PagingData) = transaction {
        val categories = CategoryEntity.all().limit(paging.limit, paging.offset)
        return@transaction categories.map {
            it.categoryResponse()
        }
    }

    fun updateCategory(updateCategory: UpdateCategory) = transaction {
        val categoryExist =
            CategoryEntity.find { CategoryTable.id eq updateCategory.categoryId }.toList().singleOrNull()
        categoryExist?.let {
            it.categoryName = updateCategory.categoryName
            // return category response
            it.categoryResponse()
        } ?: run {
            throw CommonException("Category not  exist")
        }
    }

    fun deleteCategory(deleteCategory: DeleteCategory) = transaction {
        val categoryExist =
            CategoryEntity.find { CategoryTable.id eq deleteCategory.categoryId }.toList().singleOrNull()
        categoryExist?.delete()
    }
}