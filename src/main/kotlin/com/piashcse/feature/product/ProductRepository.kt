package com.piashcse.feature.product

import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.Product

interface ProductRepository {
    /**
     * Creates a new product.
     *
     * @param userId The unique identifier of the user creating the product.
     * @param shopId The shop where the product will be listed
     * @param productRequest The details of the product to create.
     * @return The created product.
     */
    suspend fun createProduct(userId: String, shopId: String?, productRequest: ProductRequest): Product

    /**
     * Updates an existing product.
     *
     * @param userId The unique identifier of the user updating the product.
     * @param productId The unique identifier of the product to update.
     * @param update The product details to update.
     * @return The updated product.
     */
    suspend fun updateProduct(userId: String, productId: String, updateProduct: UpdateProductRequest): Product

    /**
     * Retrieves a list of products based on filters.
     *
     * @param query The filters to apply when retrieving products.
     * @return A list of products matching the filters.
     */
    suspend fun getProducts(productQuery: ProductWithFilterRequest): List<Product>

    /**
     * Retrieves products by shop ID (for seller dashboard).
     *
     * @param shopId The shop ID to retrieve products for.
     * @param query The filters to apply when retrieving the product.
     * @return A list of products for the specific shop.
     */
    suspend fun getProductsByShop(shopId: String, productQuery: ProductWithFilterRequest): List<Product>

    /**
     * Retrieves products by seller/user ID.
     *
     * @param userId The unique identifier of the user.
     * @param query The filters to apply when retrieving the product.
     * @return A list of products (even if only one product matches).
     */
    suspend fun getProductsByUser(userId: String, productQuery: ProductWithFilterRequest): List<Product>

    /**
     * Retrieves detailed information about a specific product.
     *
     * @param productId The unique identifier of the product.
     * @return The product details.
     */
    suspend fun getProductDetail(productId: String): Product

    /**
     * Increments the view count for a product.
     *
     * @param productId The unique identifier of the product.
     */
    suspend fun incrementViewCount(productId: String)

    /**
     * Deletes a specific product.
     *
     * @param userId The unique identifier of the user deleting the product.
     * @param productId The unique identifier of the product to delete.
     * @return A confirmation message.
     */
    suspend fun deleteProduct(userId: String, productId: String): String

    /**
     * Searches for products based on query parameters.
     *
     * @param query The search parameters.
     * @return A list of products matching the search.
     */
    suspend fun searchProduct(productQuery: ProductSearchRequest): List<Product>

    /**
     * Gets products by category.
     *
     * @param categoryId The category ID to filter by.
     * @return A list of products in the category.
     */
    suspend fun getProductsByCategory(categoryId: String): List<Product>

    /**
     * Gets featured products.
     *
     * @return A list of featured products.
     */
    suspend fun getFeaturedProducts(): List<Product>

    /**
     * Gets best selling products.
     *
     * @return A list of best selling products.
     */
    suspend fun getBestSellingProducts(): List<Product>

    /**
     * Gets hot deals.
     *
     * @return A list of hot deal products.
     */
    suspend fun getHotDealProducts(): List<Product>
}