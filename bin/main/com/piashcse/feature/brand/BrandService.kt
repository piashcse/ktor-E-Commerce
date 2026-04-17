package com.piashcse.feature.brand

import com.piashcse.constants.Message
import com.piashcse.database.entities.BrandDAO
import com.piashcse.database.entities.BrandTable
import com.piashcse.model.response.Brand
import com.piashcse.utils.PaginatedResponse
import com.piashcse.utils.PaginationMetadata
import com.piashcse.utils.ValidationException
import com.piashcse.utils.throwConflict
import com.piashcse.utils.throwNotFound
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Controller for managing brand-related operations.
 */
class BrandService : BrandRepository {
    /**
     * Creates a new brand if it does not already exist.
     *
     * @param name The name of the brand to be created.
     * @return The created brand entity.
     * @throws Exception if the brand name already exists.
     */
    override suspend fun createBrand(name: String): Brand = query {
        if (name.isBlank()) {
            throw ValidationException(Message.Brands.BLANK_NAME)
        }
        if (name.length > 255) {
            throw ValidationException(Message.Brands.nameTooLong(255))
        }

        val isBrandExist = BrandDAO.find { BrandTable.name eq name }.singleOrNull()
        isBrandExist?.let {
            throw name.throwConflict("Brand")
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
    override suspend fun getBrands(limit: Int, offset: Int): PaginatedResponse<Brand> = query {
        val query = BrandTable.selectAll()
        val totalCount = query.count()
        val data = query.limit(limit)
            .offset(offset.toLong())
            .map { BrandDAO.wrapRow(it).response() }

        PaginatedResponse(
            data = data,
            metadata = PaginationMetadata(
                totalCount = totalCount,
                limit = limit,
                skip = offset
            )
        )
    }

    /**
     * Updates the name of an existing brand.
     *
     * @param brandId The ID of the brand to update.
     * @param name The new name for the brand.
     * @return The updated brand entity.
     * @throws Exception if the brand ID is not found.
     */
    override suspend fun updateBrand(brandId: String, name: String): Brand = query {
        if (name.isBlank()) {
            throw ValidationException(Message.Brands.BLANK_NAME)
        }
        if (name.length > 255) {
            throw ValidationException(Message.Brands.nameTooLong(255))
        }

        val brand = BrandDAO.find { BrandTable.id eq brandId }.singleOrNull()
            ?: brandId.throwNotFound("Brand")

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
    override suspend fun deleteBrand(brandId: String): String = query {
        val isBrandExist = BrandDAO.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.delete()
            brandId
        } ?: brandId.throwNotFound("Brand")
    }
}