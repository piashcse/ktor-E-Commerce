package com.piashcse.modules.brand.repository

import com.piashcse.database.entities.Brand

interface BrandRepo {
    /**
     * Creates a new brand.
     *
     * @param name The name of the brand.
     * @return The created brand.
     */
    suspend fun createBrand(name: String): Brand
    /**
     * Retrieves all available brands with a limit.
     *
     * @param limit The maximum number of brands to return.
     * @return A list of brands.
     */
    suspend fun getBrands(limit: Int): List<Brand>
    /**
     * Updates an existing brand.
     *
     * @param brandId The unique identifier of the brand.
     * @param name The updated name of the brand.
     * @return The updated brand.
     */
    suspend fun updateBrand(brandId: String, name: String): Brand
    /**
     * Deletes a brand by its ID.
     *
     * @param brandId The unique identifier of the brand.
     * @return A confirmation message.
     */
    suspend fun deleteBrand(brandId: String): String
}