package com.piashcse.controller

import com.piashcse.dbhelper.query
import com.piashcse.entities.product.category.ProductCategoryEntity
import com.piashcse.entities.product.category.ProductCategoryTable
import com.piashcse.models.PagingData
import com.piashcse.models.category.AddProductCategory
import com.piashcse.models.category.DeleteProductCategory
import com.piashcse.models.category.UpdateProductCategory
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.isNotExistException
import org.jetbrains.exposed.sql.transactions.transaction

class ProductCategoryController {
   suspend fun createProductCategory(addProductCategory: AddProductCategory) = query {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.categoryName eq addProductCategory.categoryName }.toList().singleOrNull()

        if (categoryExist == null) {
            ProductCategoryEntity.new {
                categoryName = addProductCategory.categoryName
            }.response()
        } else {
            addProductCategory.categoryName.alreadyExistException()
        }
    }

    suspend fun getProductCategory(paging: PagingData) = query {
        val categories = ProductCategoryEntity.all().limit(paging.limit, paging.offset)
        categories.map {
            it.response()
        }
    }

   suspend fun updateProductCategory(updateProductCategory: UpdateProductCategory) = query {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq updateProductCategory.categoryId }.toList().singleOrNull()
        categoryExist?.let {
            it.categoryName = updateProductCategory.categoryName
            // return category response
            it.response()
        } ?: run {
            updateProductCategory.categoryId.isNotExistException()
        }
    }

   suspend fun deleteProductCategory(deleteProductCategory: DeleteProductCategory) = query {
        val categoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq deleteProductCategory.categoryId }.toList().singleOrNull()
        categoryExist?.let {
            categoryExist.delete()
            deleteProductCategory.categoryId
        }
    }
}