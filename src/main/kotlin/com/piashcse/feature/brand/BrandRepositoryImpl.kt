package com.piashcse.feature.brand

import com.piashcse.constants.Message
import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.mapper.toBrandResponse
import com.piashcse.model.response.BrandResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

class BrandRepositoryImpl : BrandRepository {
    companion object {
        private const val MAX_NAME_LENGTH = 255
    }

    override suspend fun createBrand(name: String): BrandResponse =
        query {
            if (name.isBlank()) {
                throw ValidationException(Message.Brands.BLANK_NAME)
            }
            if (name.length > MAX_NAME_LENGTH) {
                throw ValidationException(Message.Brands.nameTooLong(MAX_NAME_LENGTH))
            }

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
            if (name.isBlank()) {
                throw ValidationException(Message.Brands.BLANK_NAME)
            }
            if (name.length > MAX_NAME_LENGTH) {
                throw ValidationException(Message.Brands.nameTooLong(MAX_NAME_LENGTH))
            }

            val brand =
                BrandDAO.findById(brandId)
                    ?: brandId.throwNotFound("BrandResponse")

            brand.name = name
            brand.toBrandResponse()
        }

    override suspend fun deleteBrand(brandId: String): String =
        query {
            val isBrandExist = BrandDAO.findById(brandId)
            isBrandExist?.let {
                it.delete()
                brandId
            } ?: brandId.throwNotFound("BrandResponse")
        }
}
