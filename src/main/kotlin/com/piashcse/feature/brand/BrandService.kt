package com.piashcse.feature.brand

import com.piashcse.constants.Message
import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.model.response.BrandResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwConflict
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Service for managing brand-related operations.
 */
class BrandService : BrandRepository {
    /**
     * Creates a new brand if it does not already exist.
     *
     * @param name The name of the brand to be created.
     * @return The created brand entity.
     * @throws Exception if the brand name already exists.
     */
    override suspend fun createBrand(name: String): BrandResponse =
        query {
            if (name.isBlank()) {
                throw ValidationException(Message.Brands.BLANK_NAME)
            }
            if (name.length > 255) {
                throw ValidationException(Message.Brands.nameTooLong(255))
            }

            val isBrandExist = BrandDAO.find { BrandTable.name eq name }.firstOrNull()
            isBrandExist?.let {
                throw name.throwConflict("BrandResponse")
            } ?: BrandDAO.new {
                this.name = name
            }.response()
        }

    /**
     * Retrieves a list of brands with a specified limit.
     *
     * @param limit The maximum number of brands to retrieve.
     * @return A list of brand entities.
     */
    override suspend fun getBrands(
        limit: Int,
        offset: Int,
    ): PaginatedResponse<BrandResponse> =
        query {
            BrandTable.selectAll().toPaginatedResponse(limit, offset) {
                BrandDAO.wrapRow(it).response()
            }
        }

    /**
     * Updates the name of an existing brand.
     *
     * @param brandId The ID of the brand to update.
     * @param name The new name for the brand.
     * @return The updated brand entity.
     * @throws Exception if the brand ID is not found.
     */
    override suspend fun updateBrand(
        brandId: String,
        name: String,
    ): BrandResponse =
        query {
            if (name.isBlank()) {
                throw ValidationException(Message.Brands.BLANK_NAME)
            }
            if (name.length > 255) {
                throw ValidationException(Message.Brands.nameTooLong(255))
            }

            val brand =
                BrandDAO.findById(brandId)
                    ?: brandId.throwNotFound("BrandResponse")

            brand.name = name
            brand.response()
        }

    /**
     * Deletes a brand by its ID.
     *
     * @param brandId The ID of the brand to delete.
     * @return The ID of the deleted brand.
     * @throws Exception if the brand ID is not found.
     */
    override suspend fun deleteBrand(brandId: String): String =
        query {
            val isBrandExist = BrandDAO.findById(brandId)
            isBrandExist?.let {
                it.delete()
                brandId
            } ?: brandId.throwNotFound("BrandResponse")
        }
}
