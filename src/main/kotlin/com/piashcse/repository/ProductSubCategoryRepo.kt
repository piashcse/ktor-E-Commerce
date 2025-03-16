package com.piashcse.repository

import com.piashcse.entities.ProductSubCategory
import com.piashcse.models.subcategory.AddProductSubCategory

interface ProductSubCategoryRepo {
    suspend fun addProductSubCategory(productSubCategory: AddProductSubCategory): ProductSubCategory
    suspend fun getProductSubCategory(categoryId: String, limit: Int): List<ProductSubCategory>
    suspend fun updateProductSubCategory(id: String, subCategoryName: String): ProductSubCategory
    suspend fun deleteProductSubCategory(subCategoryId: String): String
}