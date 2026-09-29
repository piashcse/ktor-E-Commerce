package com.piashcse.feature.brand

import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.mapper.toBrandResponse
import com.piashcse.model.response.BrandResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireValidName
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class BrandRepositoryImpl : BrandRepository {

    override suspend fun createBrand(name: String): BrandResponse =
        query {
            name.requireValidName("Brand")

            val isBrandExist = BrandDAO.find { BrandTable.name eq name }.firstOrNull()
            isBrandExist?.let {
                throw name.throwConflict("BrandResponse")
            } ?: BrandDAO.new {
                this.name = name
            }.toBrandResponse()
        }

    override suspend fun getBrands(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<BrandResponse> =
        query {
            BrandTable.selectAll().toPaginatedResponse(limit, offset) {
                BrandDAO.wrapRow(it).toBrandResponse()
            }
        }

    override suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse =
        query {
            name.requireValidName("Brand")

            val brand =
                BrandDAO.findById(brandId)
                    ?: brandId.throwNotFound("BrandResponse")

            brand.name = name
            brand.toBrandResponse()
        }

    override suspend fun deleteBrand(brandId: String): String =
        query {
            val brand = BrandDAO.findById(brandId) ?: brandId.throwNotFound("BrandResponse")
            // Guard vs ON DELETE SET NULL/CASCADE data loss: block while products still reference this brand.
            if (!ProductDAO.find { ProductTable.brandId eq brandId }.empty()) {
                throw com.piashcse.utils.validator.ConflictException("Cannot delete brand: products still reference it. Reassign or soft-delete products first.")
            }
            brand.delete()
            brandId
        }
}
