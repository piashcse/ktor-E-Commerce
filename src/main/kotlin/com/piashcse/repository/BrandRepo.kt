package com.piashcse.repository

import com.piashcse.entities.Brand

interface BrandRepo {
    suspend fun addBrand(brandName: String): Brand
    suspend fun getBrands(limit: Int, offset: Long): List<Brand>
    suspend fun updateBrand(brandId: String, brandName: String): Brand
    suspend fun deleteBrand(brandId: String): String
}