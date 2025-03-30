package com.piashcse.controller

import com.piashcse.entities.*
import com.piashcse.models.product.request.ProductRequest
import com.piashcse.models.product.request.ProductSearchRequest
import com.piashcse.models.product.request.ProductWithFilterRequest
import com.piashcse.models.product.request.UpdateProduct
import com.piashcse.repository.ProductRepo
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import java.io.File

/**
 * Controller for managing products.
 */
class ProductController : ProductRepo {

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
     * @param productRequest The request object containing product details.
     * @return The created product entity.
     */
    override suspend fun createProduct(userId: String, productRequest: ProductRequest): Product = query {
        ProductEntity.new {
            this.userId = EntityID(userId, ProductTable)
            categoryId = EntityID(productRequest.categoryId, ProductTable)
            subCategoryId = productRequest.subCategoryId?.let { EntityID(productRequest.subCategoryId, ProductTable) }
            brandId = productRequest.brandId?.let { EntityID(productRequest.brandId, ProductTable) }
            productName = productRequest.productName
            productCode = productRequest.productCode
            productQuantity = productRequest.productQuantity
            productDetail = productRequest.productDetail
            price = productRequest.price
            discountPrice = productRequest.discountPrice
            status = productRequest.status
            videoLink = productRequest.videoLink
            mainSlider = productRequest.mainSlider
            hotDeal = productRequest.hotDeal
            bestRated = productRequest.bestRated
            midSlider = productRequest.midSlider
            hotNew = productRequest.hotNew
            trend = productRequest.trend
            buyOneGetOne = productRequest.buyOneGetOne
            imageOne = productRequest.imageOne
            imageTwo = productRequest.imageTwo
        }.response()
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
    override suspend fun updateProduct(userId: String, productId: String, updateProduct: UpdateProduct): Product =
        query {
            val isProductExist =
                ProductEntity.find { ProductTable.userId eq userId and (ProductTable.id eq productId) }.toList()
                    .singleOrNull()
            isProductExist?.apply {
                this.userId = EntityID(userId, ProductTable)
                categoryId =
                    updateProduct.categoryId?.let { EntityID(updateProduct.categoryId, ProductTable) } ?: categoryId
                subCategoryId = updateProduct.subCategoryId?.let { EntityID(updateProduct.subCategoryId, ProductTable) }
                    ?: subCategoryId
                brandId = updateProduct.brandId?.let { EntityID(updateProduct.brandId, ProductTable) } ?: brandId
                productName = updateProduct.productName ?: productName
                productCode = updateProduct.productCode ?: productCode
                productQuantity = updateProduct.productQuantity ?: productQuantity
                productDetail = updateProduct.productDetail ?: productDetail
                price = updateProduct.price ?: price
                discountPrice = updateProduct.discountPrice ?: discountPrice
                status = updateProduct.status ?: status
                videoLink = updateProduct.videoLink ?: videoLink
                mainSlider = updateProduct.mainSlider ?: mainSlider
                hotDeal = updateProduct.hotDeal ?: hotDeal
                bestRated = updateProduct.bestRated ?: bestRated
                midSlider = updateProduct.midSlider ?: midSlider
                hotNew = updateProduct.hotNew ?: hotNew
                trend = updateProduct.trend ?: trend
                buyOneGetOne = updateProduct.buyOneGetOne ?: buyOneGetOne
                imageOne = updateProduct.imageOne ?: imageOne
                imageTwo = updateProduct.imageTwo ?: imageTwo
            }?.response() ?: throw productId.notFoundException()
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
            query.andWhere { ProductTable.price lessEq it }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq it
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
        query.limit(productQuery.limit).map {
            ProductEntity.wrapRow(it).response()
        }
    }

    /**
     * Retrieves a product by its ID and user ID.
     *
     * @param userId The ID of the user requesting the product.
     * @param productQuery The filter request containing product details.
     * @return A list of products matching the provided user and filter criteria.
     */
    override suspend fun getProductById(userId: String, productQuery: ProductWithFilterRequest): List<Product> = query {
        val query = ProductTable.selectAll()
        query.andWhere { ProductTable.userId eq userId }

        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq it }
        }
        productQuery.maxPrice?.let {
            query.andWhere { ProductTable.price lessEq it }
        }
        productQuery.minPrice?.let {
            query.andWhere {
                ProductTable.price greaterEq it
            }
        }
        productQuery.categoryId.let {
            query.adjustWhere {
                ProductTable.categoryId eq it
            }
        }
        productQuery.subCategoryId.let {
            query.adjustWhere {
                ProductTable.subCategoryId eq it
            }
        }
        productQuery.brandId?.let {
            query.adjustWhere {
                ProductTable.brandId eq it
            }
        }
        query.limit(productQuery.limit).map {
            ProductEntity.wrapRow(it).response()
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
        val isProductExist = ProductEntity.find { ProductTable.id eq productId }.toList().singleOrNull()
        isProductExist?.response() ?: throw productId.notFoundException()
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
        val isProductExist =
            ProductEntity.find { ProductTable.userId eq userId and (ProductTable.id eq productId) }.toList()
                .singleOrNull()
        isProductExist?.let {
            it.delete()
            productId
        } ?: throw productId.notFoundException()
    }

    /**
     * Uploads an image for a product.
     *
     * @param userId The ID of the user uploading the image.
     * @param productId The ID of the product to associate the image with.
     * @param productImage The image URL to be uploaded.
     * @return The entity representing the uploaded product image.
     */
    override suspend fun uploadProductImage(userId: String, productId: String, productImage: String): ImageUrl = query {
        val isImageExist = ProductImageEntity.find { ProductImageTable.productId eq productId }.toList().singleOrNull()
        isImageExist?.let {
            File("${AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION}${it.imageUrl}").delete()
            it.imageUrl = productImage
            it.response()
        } ?: ProductImageEntity.new {
            this.userId = EntityID(userId, ProductTable)
            this.productId = EntityID(productId, ProductTable)
            this.imageUrl = productImage
        }.response()
    }

    /**
     * Searches for products based on the given search criteria.
     *
     * @param productQuery The search request containing the parameters for searching products.
     * @return A list of products matching the search criteria.
     */
    override suspend fun searchProduct(productQuery: ProductSearchRequest): List<Product> = query {
        ProductEntity.find {
            // Apply filters dynamically based on query parameters
            val conditions = mutableListOf<Op<Boolean>>()

            if (productQuery.productName.isNotEmpty()) {
                conditions.add(ProductTable.productName like "%$productQuery.productName%")
            }
            if (!productQuery.categoryId.isNullOrEmpty()) {
                conditions.add(ProductTable.categoryId eq productQuery.categoryId)
            }
            if (productQuery.maxPrice != null) {
                conditions.add(ProductTable.price greaterEq productQuery.maxPrice)
            }
            if (productQuery.minPrice != null) {
                conditions.add(ProductTable.price lessEq productQuery.minPrice )
            }

            // Combine all conditions with AND logic
            if (conditions.isEmpty()) Op.TRUE else conditions.reduce { acc, op -> acc and op }
        }.map { it.response() }
    }
}