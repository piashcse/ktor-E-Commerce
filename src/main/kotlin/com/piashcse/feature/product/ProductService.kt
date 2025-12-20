package com.piashcse.feature.product

import com.piashcse.constants.AppConstants
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.Product
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.io.File

/**
 * Controller for managing products.
 */
class ProductService : ProductRepository {

    /**
     * Initializes the product image folder if it does not exist.
     */
    init {
        if (!File(AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION).exists()) {
            File(AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION).mkdirs()
        }
    }

    /**
     * Creates a new product.
     *
     * @param userId The ID of the user creating the product.
     * @param shopId The shop where the product will be listed
     * @param productRequest The request object containing product details.
     * @return The created product entity.
     */
    override suspend fun createProduct(userId: String, shopId: String?, productRequest: ProductRequest): Product = query {
        validateProductForSeller(userId, shopId)

        val product = ProductDAO.new {
            this.userId = EntityID(userId, ProductTable)
            this.shopId = shopId?.let { EntityID(shopId, ProductTable) }
            categoryId = EntityID(productRequest.categoryId, ProductTable)
            subCategoryId = productRequest.subCategoryId?.let { EntityID(productRequest.subCategoryId, ProductTable) }
            brandId = productRequest.brandId?.let { EntityID(productRequest.brandId, ProductTable) }
            sku = generateSKU(productRequest.name)
            name = productRequest.name
            description = productRequest.description
            price = java.math.BigDecimal.valueOf(productRequest.price)
            discountPrice = productRequest.discountPrice?.let { java.math.BigDecimal.valueOf(it) }
            discountPercentage = calculateDiscountPercentage(productRequest.price, productRequest.discountPrice)
            stockQuantity = productRequest.stockQuantity
            videoLink = productRequest.videoLink
            hotDeal = productRequest.hotDeal
            featured = productRequest.featured
            bestSeller = false
            newProduct = true
            freeShipping = productRequest.freeShipping ?: false
            images = productRequest.images.joinToString(",")
            status = ProductStatus.ACTIVE
        }.response()

        product
    }

    private fun validateProductForSeller(userId: String, shopId: String?) {
        // Verify that the user is a seller
        val seller = SellerDAO.find {
            SellerTable.userId eq userId
        }.singleOrNull()
        if (seller == null) {
            throw "User is not registered as a seller".notFoundException()
        }

        // If shopId is provided, verify that the user is authorized to add products to that shop
        if (shopId != null) {
            val shop = ShopDAO.find {
                ShopTable.id eq shopId and
                (ShopTable.userId eq userId)
            }.singleOrNull()
            if (shop == null) {
                throw "User is not authorized to add products to this shop".notFoundException()
            }
        }
    }


    private fun generateSKU(productName: String): String {
        val cleanName = productName.replace(Regex("[^a-zA-Z0-9]"), "").take(6).uppercase()
        val timestamp = System.currentTimeMillis().toString().takeLast(6)
        return "${cleanName}${timestamp}"
    }

    private fun calculateDiscountPercentage(price: Double, discountPrice: Double?): java.math.BigDecimal? {
        return if (discountPrice != null && discountPrice < price) {
            val discount = price - discountPrice
            val percentage = (discount / price) * 100
            java.math.BigDecimal.valueOf(percentage).setScale(2, java.math.RoundingMode.HALF_UP)
        } else null
    }

    /**
     * Updates an existing product's details.
     *
     * @param userId The ID of the user updating the product.
     * @param productId The ID of the product to be updated.
     * @param updateProduct The update request object containing new product details.
     * @return The updated product entity.
     * @throws Exception if the product with the provided ID is not found.
     */
    override suspend fun updateProduct(userId: String, productId: String, updateProduct: UpdateProductRequest): Product =
        query {
            // Verify that the user is a seller
            val seller = SellerDAO.find {
                SellerTable.userId eq userId
            }.singleOrNull()
            if (seller == null) {
                throw "User is not registered as a seller".notFoundException()
            }

            // Verify user has access to this product
            val product = ProductDAO.find { ProductTable.userId eq userId and (ProductTable.id eq productId) }.singleOrNull()
                ?: throw productId.notFoundException()

            product.apply {
                this.userId = EntityID(userId, ProductTable)
                categoryId =
                    updateProduct.categoryId?.let { EntityID(updateProduct.categoryId, ProductTable) } ?: categoryId
                subCategoryId = updateProduct.subCategoryId?.let { EntityID(updateProduct.subCategoryId, ProductTable) }
                    ?: subCategoryId
                brandId = updateProduct.brandId?.let { EntityID(updateProduct.brandId, ProductTable) } ?: brandId
                name = updateProduct.name ?: name
                description = updateProduct.description ?: description
                // Don't update sku, barcode, weight, dimensions, minOrderQuantity, discountPercentage, bestSeller, newProduct, viewCount, rating, totalReviews, totalSales
                price = updateProduct.price?.let { java.math.BigDecimal.valueOf(it) } ?: price
                discountPrice = updateProduct.discountPrice?.let { java.math.BigDecimal.valueOf(it) } ?: discountPrice
                stockQuantity = updateProduct.stockQuantity ?: stockQuantity
                videoLink = updateProduct.videoLink ?: videoLink
                hotDeal = updateProduct.hotDeal ?: hotDeal
                featured = updateProduct.featured ?: featured
                freeShipping = updateProduct.freeShipping ?: freeShipping
                images = if (updateProduct.images.isEmpty()) images else updateProduct.images.joinToString(",")
            }.response()
        }

    /**
     * Retrieves a list of products based on the provided filter criteria.
     *
     * @param productQuery The filter request containing the parameters to filter products.
     * @return A list of products matching the provided filters.
     */
    override suspend fun getProducts(productQuery: ProductWithFilterRequest): List<Product> = query {
        val query = ProductTable.selectAll()
        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq java.math.BigDecimal.valueOf(it) }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq java.math.BigDecimal.valueOf(it)
            }
        }
        productQuery.categoryId?.let {
            query.adjustWhere {
                ProductTable.categoryId eq it
            }
        }
        productQuery.subCategoryId?.let {
            query.adjustWhere {
                ProductTable.subCategoryId eq it
            }
        }
        productQuery.brandId?.let {
            query.adjustWhere {
                ProductTable.brandId eq it
            }
        }
        query.andWhere { ProductTable.status eq ProductStatus.ACTIVE }
        query.limit(productQuery.limit).map {
            ProductDAO.wrapRow(it).response()
        }
    }

    /**
     * Retrieves products by shop ID (for seller dashboard).
     *
     * @param shopId The shop ID to retrieve products for.
     * @param productQuery The filter request containing the parameters to filter products.
     * @return A list of products for the specific shop.
     */
    override suspend fun getProductsByShop(shopId: String, productQuery: ProductWithFilterRequest): List<Product> = query {
        val query = ProductTable.selectAll()
        query.andWhere { ProductTable.shopId eq shopId }

        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq java.math.BigDecimal.valueOf(it) }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq java.math.BigDecimal.valueOf(it)
            }
        }
        productQuery.categoryId?.let {
            query.adjustWhere {
                ProductTable.categoryId eq it
            }
        }
        productQuery.subCategoryId?.let {
            query.adjustWhere {
                ProductTable.subCategoryId eq it
            }
        }
        productQuery.brandId?.let {
            query.adjustWhere {
                ProductTable.brandId eq it
            }
        }
        query.andWhere { ProductTable.status eq ProductStatus.ACTIVE }
        query.limit(productQuery.limit).map {
            ProductDAO.wrapRow(it).response()
        }
    }

    /**
     * Retrieves products by seller/user ID.
     *
     * @param userId The unique identifier of the user.
     * @param productQuery The filter request containing product details.
     * @return A list of products (even if only one product matches).
     */
    override suspend fun getProductsByUser(userId: String, productQuery: ProductWithFilterRequest): List<Product> = query {
        val query = ProductTable.selectAll()
        query.andWhere { ProductTable.userId eq userId }

        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq java.math.BigDecimal.valueOf(it) }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq java.math.BigDecimal.valueOf(it)
            }
        }
        productQuery.categoryId?.let {
            query.adjustWhere {
                ProductTable.categoryId eq it
            }
        }
        productQuery.subCategoryId?.let {
            query.adjustWhere {
                ProductTable.subCategoryId eq it
            }
        }
        productQuery.brandId?.let {
            query.adjustWhere {
                ProductTable.brandId eq it
            }
        }
        query.andWhere { ProductTable.status eq ProductStatus.ACTIVE }
        query.limit(productQuery.limit).map {
            ProductDAO.wrapRow(it).response()
        }
    }

    /**
     * Retrieves detailed information for a specific product.
     *
     * @param productId The ID of the product to retrieve.
     * @return The product entity with full details.
     * @throws Exception if the product with the provided ID is not found.
     */
    override suspend fun getProductDetail(productId: String): Product = query {
        val product = ProductDAO.find { ProductTable.id eq productId }.singleOrNull()
        product?.response() ?: throw productId.notFoundException()
    }

    /**
     * Deletes a product by its ID and user ID.
     *
     * @param userId The ID of the user deleting the product.
     * @param productId The ID of the product to be deleted.
     * @return The ID of the deleted product.
     * @throws Exception if the product with the provided ID is not found.
     */
    override suspend fun deleteProduct(userId: String, productId: String): String = query {
        validateProductForSeller(userId, null) // Only check if user is a seller

        // Find the product and verify user ownership
        val product = ProductDAO.find {
            ProductTable.userId eq userId and (ProductTable.id eq productId)
        }.singleOrNull() ?: throw productId.notFoundException()

        product.delete()
        productId
    }

    /**
     * Increments the view count for a product.
     *
     * @param productId The unique identifier of the product.
     */
    override suspend fun incrementViewCount(productId: String): Unit = query {
        val product = ProductDAO.find { ProductTable.id eq productId }.singleOrNull()
        product?.apply {
            viewCount = viewCount + 1
        }
    }

    /**
     * Searches for products based on the given search criteria.
     *
     * @param productQuery The search request containing the parameters for searching products.
     * @return A list of products matching the search criteria.
     */
    override suspend fun searchProduct(productQuery: ProductSearchRequest): List<Product> = query {
        var products = ProductDAO.all().toList()

        if (productQuery.name.isNotEmpty()) {
            products = products.filter { it.name.contains(productQuery.name, ignoreCase = true) }
        }
        if (!productQuery.categoryId.isNullOrEmpty()) {
            products = products.filter { it.categoryId.value == productQuery.categoryId }
        }
        if (productQuery.maxPrice != null) {
            products = products.filter { it.price.toDouble() <= productQuery.maxPrice }
        }
        if (productQuery.minPrice != null) {
            products = products.filter { it.price.toDouble() >= productQuery.minPrice }
        }

        return@query products.map { it.response() }
    }

    /**
     * Gets products by category.
     *
     * @param categoryId The category ID to filter by.
     * @return A list of products in the category.
     */
    override suspend fun getProductsByCategory(categoryId: String): List<Product> = query {
        ProductDAO.find {
            ProductTable.categoryId eq categoryId and (ProductTable.status eq ProductStatus.ACTIVE)
        }.map { it.response() }
    }

    /**
     * Gets featured products.
     *
     * @return A list of featured products.
     */
    override suspend fun getFeaturedProducts(): List<Product> = query {
        ProductDAO.find {
            ProductTable.featured eq true and (ProductTable.status eq ProductStatus.ACTIVE)
        }.orderBy(ProductTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC).map { it.response() }
    }

    /**
     * Gets best selling products.
     *
     * @return A list of best selling products.
     */
    override suspend fun getBestSellingProducts(): List<Product> = query {
        ProductDAO.find {
            ProductTable.bestSeller eq true and (ProductTable.status eq ProductStatus.ACTIVE)
        }.orderBy(ProductTable.totalSales to org.jetbrains.exposed.v1.core.SortOrder.DESC).limit(10).map { it.response() }
    }

    /**
     * Gets hot deals.
     *
     * @return A list of hot deal products.
     */
    override suspend fun getHotDealProducts(): List<Product> = query {
        ProductDAO.find {
            ProductTable.hotDeal eq true and (ProductTable.status eq ProductStatus.ACTIVE)
        }.orderBy(ProductTable.discountPercentage to org.jetbrains.exposed.v1.core.SortOrder.DESC).limit(10).map { it.response() }
    }
}