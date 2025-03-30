package com.piashcse.repository

import com.piashcse.entities.ProductSubCategory
import com.piashcse.models.subcategory.ProductSubCategoryRequest

interface ProductSubCategoryRepo {
    suspend fun addProductSubCategory(productSubCategory: ProductSubCategoryRequest): ProductSubCategory
    suspend fun getProductSubCategory(categoryId: String, limit: Int): List<ProductSubCategory>
    suspend fun updateProductSubCategory(id: String, subCategoryName: String): ProductSubCategory
    suspend fun deleteProductSubCategory(subCategoryId: String): String
}