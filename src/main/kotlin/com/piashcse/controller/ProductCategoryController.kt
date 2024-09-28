package com.piashcse.controller

import com.piashcse.entities.product.ProductCategory
import com.piashcse.entities.product.ProductCategoryEntity
import com.piashcse.entities.product.ProductCategoryTable
import com.piashcse.repository.ProductCategoryRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query

class ProductCategoryController : ProductCategoryRepo {
    override suspend fun addProductCategory(categoryName: String): ProductCategory = query {
        val isCategoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.categoryName eq categoryName }.toList().singleOrNull()
        isCategoryExist?.let {
            throw categoryName.alreadyExistException()
        } ?: ProductCategoryEntity.new {
            this.categoryName = categoryName
        }.response()
    }

    override suspend fun getProductCategory(limit: Int, offset: Long): List<ProductCategory> = query {
        val categories = ProductCategoryEntity.all().limit(limit, offset)
        categories.map {
            it.response()
        }
    }

    override suspend fun updateProductCategory(categoryId: String, categoryName: String): ProductCategory = query {
        val isCategoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq categoryId }.toList().singleOrNull()
        isCategoryExist?.let {
            it.categoryName = categoryName
            it.response()
        } ?: throw categoryId.notFoundException()
    }

    override suspend fun deleteProductCategory(categoryId: String): String = query {
        val isCategoryExist =
            ProductCategoryEntity.find { ProductCategoryTable.id eq categoryId }.toList().singleOrNull()
        isCategoryExist?.let {
            isCategoryExist.delete()
            categoryId
        } ?: throw categoryId.notFoundException()
    }
}