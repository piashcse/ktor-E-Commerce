package com.piashcse.feature.brand

import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.mapper.toBrandResponse
import com.piashcse.model.response.BrandResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.throwConflict
import org.jetbrains.exposed.v1.core.eq

class BrandRepositoryImpl : BrandRepository,
    BaseCrudRepository<BrandDAO, BrandResponse>(BrandDAO, BrandTable, "BrandResponse") {

    override fun BrandDAO.toResponse(): BrandResponse = toBrandResponse()

    override suspend fun createBrand(name: String): BrandResponse {
        if (exists { BrandTable.name eq name }) throw name.throwConflict("Brand")
        return create { this.name = name }
    }

    override suspend fun getBrands(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<BrandResponse> = getAll(limit, offset)

    override suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse = update(brandId) { this.name = name }

    override suspend fun deleteBrand(brandId: String): String = delete(brandId)
}
