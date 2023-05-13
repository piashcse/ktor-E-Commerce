package com.piashcse.controller

import com.piashcse.entities.category.CategoryEntity
import com.piashcse.entities.category.CategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.category.AddCategory
import com.piashcse.models.category.DeleteCategory
import com.piashcse.models.category.UpdateCategory
import com.piashcse.utils.CommonException
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