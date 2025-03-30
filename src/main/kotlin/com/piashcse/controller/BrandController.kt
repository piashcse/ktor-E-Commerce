package com.piashcse.controller

import com.piashcse.entities.Brand
import com.piashcse.entities.BrandEntity
import com.piashcse.entities.BrandTable
import com.piashcse.repository.BrandRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query

/**
 * Controller for managing brand-related operations.
 */
class BrandController : BrandRepo {
    /**
     * Creates a new brand if it does not already exist.
     *
     * @param brandName The name of the brand to be created.
     * @return The created brand entity.
     * @throws Exception if the brand name already exists.
     */
    override suspend fun createBrand(brandName: String): Brand = query {
        val isBrandExist = BrandEntity.find { BrandTable.brandName eq brandName }.toList().singleOrNull()
        isBrandExist?.let {
            throw brandName.alreadyExistException()
        } ?: BrandEntity.new {
            this.brandName = brandName
        }.response()
    }

    /**
     * Retrieves a list of brands with a specified limit.
     *
     * @param limit The maximum number of brands to retrieve.
     * @return A list of brand entities.
     */
    override suspend fun getBrands(limit: Int): List<Brand> = query {
        BrandEntity.all().limit(limit).map {
            it.response()
        }
    }

    /**
     * Updates the name of an existing brand.
     *
     * @param brandId The ID of the brand to update.
     * @param brandName The new name for the brand.
     * @return The updated brand entity.
     * @throws Exception if the brand ID is not found.
     */
    override suspend fun updateBrand(brandId: String, brandName: String): Brand = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.brandName = brandName
            it.response()
        } ?: throw brandId.notFoundException()
    }

    /**
     * Deletes a brand by its ID.
     *
     * @param brandId The ID of the brand to delete.
     * @return The ID of the deleted brand.
     * @throws Exception if the brand ID is not found.
     */
    override suspend fun deleteBrand(brandId: String): String = query {
        val isBrandExist = BrandEntity.find { BrandTable.id eq brandId }.toList().singleOrNull()
        isBrandExist?.let {
            it.delete()
            brandId
        } ?: throw brandId.notFoundException()
    }
}