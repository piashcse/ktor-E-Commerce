package com.piashcse.controller

import com.piashcse.database.query
import com.piashcse.entities.product.category.ProductCategoryEntity
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.category.AddProductCategory
import com.piashcse.models.category.DeleteProductCategory
import com.piashcse.models.category.UpdateProductCategory
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException

class ProductCategoryController {
    suspend fun createProductCategory(categoryName: String) = query {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.categoryName eq categoryName }.toList().singleOrNull()

        if (categoryExist == null) {
            ProductCategoryEntity.new {
                this.categoryName = categoryName
            }.response()
        } else {
            categoryName.alreadyExistException()
        }
    }

    suspend fun getProductCategory(limit: Int, offset: Long) = query {
        val categories = ProductCategoryEntity.all().limit(limit, offset)
        categories.map {
            it.response()
        }
    }

    suspend fun updateProductCategory(categoryId: String, categoryName: String) = query {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq categoryId }.toList()
                .singleOrNull()
        categoryExist?.let {
            it.categoryName = categoryName
            // return category response
            it.response()
        } ?: run {
            categoryId.isNotExistException()
        }
    }

    suspend fun deleteProductCategory(categoryId: String) = query {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq categoryId }.toList()
                .singleOrNull()
        categoryExist?.let {
            categoryExist.delete()
            categoryId
        }
    }
}