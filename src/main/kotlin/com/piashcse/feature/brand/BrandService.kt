package com.piashcse.feature.brand

import com.piashcse.model.response.BrandResponse
import com.piashcse.utils.common.PaginatedResponse

class BrandService(private val brandRepo: BrandRepository) {
    suspend fun createBrand(name: String): BrandResponse = brandRepo.createBrand(name)

    suspend fun getBrands(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<BrandResponse> = brandRepo.getBrands(limit, offset)

    suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse = brandRepo.updateBrand(brandId, name)

    suspend fun deleteBrand(brandId: String): String = brandRepo.deleteBrand(brandId)
}
