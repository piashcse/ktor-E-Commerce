package com.piashcse.repository

import com.piashcse.entities.ImageUrl
import com.piashcse.entities.Product
import com.piashcse.models.product.request.ProductRequest
import com.piashcse.models.product.request.ProductSearchRequest
import com.piashcse.models.product.request.ProductWithFilterRequest
import com.piashcse.models.product.request.UpdateProduct

interface ProductRepo {
    /**
     * Creates a new product.
     *
     * @param userId The unique identifier of the user creating the product.
     * @param productRequest The details of the product to create.
     * @return The created product.
     */
    suspend fun createProduct(userId: String, productRequest: ProductRequest): Product

    /**
     * Updates an existing product.
     *
     * @param userId The unique identifier of the user updating the product.
     * @param productId The unique identifier of the product to update.
     * @param update The product details to update.
     * @return The updated product.
     */
    suspend fun updateProduct(userId: String, productId: String, update: UpdateProduct): Product

    /**
     * Retrieves a list of products based on filters.
     *
     * @param query The filters to apply when retrieving products.
     * @return A list of products matching the filters.
     */
    suspend fun getProducts(query: ProductWithFilterRequest): List<Product>

    /**
     * Retrieves a specific product by its ID.
     *
     * @param userId The unique identifier of the user.
     * @param query The filters to apply when retrieving the product.
     * @return A list of products (even if only one product matches).
     */
    suspend fun getProductById(userId: String, query: ProductWithFilterRequest): List<Product>

    /**
     * Retrieves detailed information about a specific product.
     *
     * @param productId The unique identifier of the product.
     * @return The product details.
     */
    suspend fun getProductDetail(productId: String): Product

    /**
     * Deletes a specific product.
     *
     * @param userId The unique identifier of the user deleting the product.
     * @param productId The unique identifier of the product to delete.
     * @return A confirmation message.
     */
    suspend fun deleteProduct(userId: String, productId: String): String

    /**
     * Uploads an image for a product.
     *
     * @param userId The unique identifier of the user uploading the image.
     * @param productId The unique identifier of the product.
     * @param imageUrl The image URL to associate with the product.
     * @return The uploaded image URL.
     */
    suspend fun uploadProductImage(userId: String, productId: String, imageUrl: String): ImageUrl

    /**
     * Searches for products based on query parameters.
     *
     * @param query The search parameters.
     * @return A list of products matching the search.
     */
    suspend fun searchProduct(query: ProductSearchRequest): List<Product>
}