package com.piashcse.feature.brand

import com.piashcse.constants.Message
import com.piashcse.model.response.BrandResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ValidationException

class BrandService(private val brandRepo: BrandRepository) {

    companion object {
        private const val MAX_NAME_LENGTH = 255
    }

    private fun validateName(name: String) {
        if (name.isBlank()) throw ValidationException(Message.Brands.BLANK_NAME)
        if (name.length > MAX_NAME_LENGTH) throw ValidationException(Message.Brands.nameTooLong(MAX_NAME_LENGTH))
    }

    /**
     * Creates a brand after validating its name. Runs in a retryable transaction.
     */
    suspend fun createBrand(name: String): BrandResponse = suspendRetryQuery {
        validateName(name)
        brandRepo.createBrand(name)
    }

    suspend fun getBrands(
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<BrandResponse> = brandRepo.getBrands(limit, offset)

    /**
     * Updates a brand after validating its name. Runs in a retryable transaction.
     */
    suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse = suspendRetryQuery {
        validateName(name)
        brandRepo.updateBrand(brandId, name)
    }

    /**
     * Deletes a brand. Runs in a retryable transaction.
     */
    suspend fun deleteBrand(brandId: String): String = suspendRetryQuery { brandRepo.deleteBrand(brandId) }
}
