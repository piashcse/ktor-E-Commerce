package com.piashcse.controller

import com.piashcse.entities.product.category.ProductCategoryEntity
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.category.AddCategory
import com.piashcse.models.category.DeleteCategory
import com.piashcse.models.category.UpdateCategory
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.sql.transactions.transaction

class ProductCategoryController {
    fun createCategory(addCategory: AddCategory) = transaction {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.categoryName eq addCategory.categoryName }.toList().singleOrNull()

        if (categoryExist == null) {
            ProductCategoryEntity.new {
                categoryName = addCategory.categoryName
            }.response()
        } else {
            addCategory.categoryName.alreadyExistException()
        }
    }

    fun getCategory(paging: PagingData) = transaction {
        val categories = ProductCategoryEntity.all().limit(paging.limit, paging.offset)
        categories.map {
            it.response()
        }
    }

    fun updateCategory(updateCategory: UpdateCategory) = transaction {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq updateCategory.categoryId }.toList().singleOrNull()
        categoryExist?.let {
            it.categoryName = updateCategory.categoryName
            // return category response
            it.response()
        } ?: run {
            updateCategory.categoryId.isNotExistException()
        }
    }

    fun deleteCategory(deleteCategory: DeleteCategory) = transaction {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq deleteCategory.categoryId }.toList().singleOrNull()
        categoryExist?.delete()
    }
}