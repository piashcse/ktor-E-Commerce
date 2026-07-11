package com.piashcse.feature.product

import com.piashcse.constants.Message
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public and seller product routes.
 */
fun Route.productRoutes() {
    val productCatalogService: ProductCatalogService by inject()
    val productRepo: ProductRepository by inject()

    rateLimit(RateLimitName(RateLimitNames.SEARCH)) {
        /**
         * @tag Product
         * @description Retrieve detailed information about a specific product
         */
        get("{id}") {
            val productId = call.requirePathParameter("id")
            call.respondOk(productCatalogService.getProductDetail(productId))
        }

        /**
         * @tag Product
         * @description Retrieve a paginated list of products with optional filters
         */
        get {
            call.respondOk(productRepo.getProducts(call.productWithFilterRequest(defaultPerPage = 10)))
        }

        /**
         * @tag Product
         * @description Search for products by name with fuzzy matching, faceted aggregation, and ranking
         */
        get("search") {
            val (limit, offset) = call.paginateQueryParams(defaultPerPage = 10)
            val queryParams =
                ProductSearchRequest(
                    limit = limit,
                    offset = offset,
                    name = call.requireQueryParameter("name"),
                    maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.request.queryParameters["categoryId"],
                    brandId = call.request.queryParameters["brandId"],
                    sortBy = call.request.queryParameters["sortBy"] ?: "relevance",
                    sortOrder = call.request.queryParameters["sortOrder"] ?: "desc",
                    useFuzzy = call.request.queryParameters["useFuzzy"]?.toBooleanStrictOrNull() ?: true,
                )
            call.respondOk(productCatalogService.searchProduct(queryParams))
        }
    }
}

/**
 * Seller product management routes.
 */
fun Route.productSellerRoutes() {
    val productRepo: ProductRepository by inject()
    val productCrudService: ProductCrudService by inject()
    /**
     * @tag Product
     * @description Seller: Retrieve seller products
     */
    get {
        call.respondOk(productRepo.getProductsByUser(call.currentUserId, call.productWithFilterRequest(defaultPerPage = 10)))
    }

    rateLimit(RateLimitName(RateLimitNames.SELLER_WRITE)) {
        /**
         * @tag Product
         * @description Seller: Add a new product listing
         */
        post {
            call.respondCreated(productCrudService.createProduct(call.currentUserId, null, call.receive<ProductRequest>()))
        }

        /**
         * @tag Product
         * @description Seller: Update an existing product listing
         */
        put("{id}") {
            val productId = call.requirePathParameter("id")
            call.respondOk(productCrudService.updateProduct(call.currentUserId, productId, call.receive<UpdateProductRequest>()))
        }

        /**
         * @tag Product
         * @description Seller: Permanently delete a product listing
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            productCrudService.deleteProduct(call.currentUserId, id)
            call.respondOk(mapOf("message" to "Product deleted successfully"))
        }

        /**
         * @tag Product
         * @description Seller: Upload a product image
         */
        post("image-upload") {
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val fileName = UploadService.uploadProductImage(part)
                    imageUrl = UploadService.getProductImageUrl(fileName)
                }
                part.dispose()
            }

            call.respondOk(imageUrl ?: throw ValidationException(Message.Validation.FILE_REQUIRED))
        }
    }
}

/**
 * Admin product management routes.
 */
fun Route.productAdminRoutes() {
    val productCrudService: ProductCrudService by inject()
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) {
        /**
         * @tag Product
         * @description Admin: Permanently delete any product
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(productCrudService.deleteProductAsAdmin(id))
        }
    }
}
