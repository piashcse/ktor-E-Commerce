package com.piashcse.feature.brand

import com.piashcse.constants.Message
import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.mapper.toBrandResponse
import com.piashcse.model.response.BrandResponse
import com.piashcse.repository.base.BaseCrudRepository
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq

class BrandRepositoryImpl : BrandRepository,
    BaseCrudRepository<BrandDAO, BrandResponse>(BrandDAO, BrandTable, "BrandResponse") {

    companion object {
        private const val MAX_NAME_LENGTH = 255
    }

    override fun BrandDAO.toResponse(): BrandResponse = toBrandResponse()

    private fun validateName(name: String) {
        if (name.isBlank()) throw ValidationException(Message.Brands.BLANK_NAME)
        if (name.length > MAX_NAME_LENGTH) throw ValidationException(Message.Brands.nameTooLong(MAX_NAME_LENGTH))
    }

    override suspend fun createBrand(name: String): BrandResponse {
        validateName(name)
        if (exists { BrandTable.name eq name }) throw name.throwConflict("BrandResponse")
        return create { this.name = name }
    }

    override suspend fun getBrands(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<BrandResponse> = getAll(limit, offset)

    override suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse {
        validateName(name)
        return update(brandId) { this.name = name }
    }

    override suspend fun deleteBrand(brandId: String): String = delete(brandId)
}
