package com.piashcse.repository

import com.piashcse.entities.ProductCategory

interface ProductCategoryRepo {
    suspend fun addProductCategory(categoryName: String): ProductCategory
    suspend fun getProductCategory(limit: Int, offset: Long): List<ProductCategory>
    suspend fun updateProductCategory(categoryId: String, categoryName: String): ProductCategory
    suspend fun deleteProductCategory(categoryId: String): String
}