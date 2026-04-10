package com.piashcse.feature.product

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Defines routes for managing products. Different routes are available based on user roles (CUSTOMER, SELLER, ADMIN).
 *
 * @param productController The controller handling product-related operations.
 */
fun Route.productRoutes(productController: ProductService) {
    route("/product") {

        /**
         * @tag Product
         * @description Retrieve detailed information about a specific product
         * @operationId getProductDetail
         * @path id (required) Unique identifier of the product
         * @response 200 Product details retrieved successfully
         */
        get("{id}") {
            val (productId) = call.requiredParameters("id") ?: return@get
            // Increment view count in background
            productController.incrementViewCount(productId)
            call.respond(ApiResponse.success(productController.getProductDetail(productId), HttpStatusCode.OK))
        }

        /**
         * @tag Product
         * @description Retrieve a paginated list of products with optional filters
         * @operationId getProducts
         * @query limit (required) Maximum number of products to return
         * @query offset Number of products to skip (for pagination)
         * @query maxPrice Filter products by maximum price
         * @query minPrice Filter products by minimum price
         * @query categoryId Filter products by category ID
         * @query subCategoryId Filter products by subcategory ID
         * @query brandId Filter products by brand ID
         * @response 200 Products retrieved successfully
         */
        get {
            val limit = call.parameters["limit"]?.toIntOrNull() ?: AppConstants.Pagination.DEFAULT_LIMIT
            val offset = call.parameters["offset"]?.toIntOrNull() ?: AppConstants.Pagination.DEFAULT_OFFSET

            val params = ProductWithFilterRequest(
                limit = limit.coerceAtMost(AppConstants.Pagination.MAX_LIMIT),
                offset = offset,
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"],
                subCategoryId = call.parameters["subCategoryId"],
                brandId = call.parameters["brandId"]
            )
            call.respond(ApiResponse.success(productController.getProducts(params), HttpStatusCode.OK))
        }

        /**
         * @tag Product
         * @description Search for products by name with optional filters
         * @operationId searchProduct
         * @query limit (required) Maximum number of products to return
         * @query name (required) Search term for product name
         * @query categoryId Filter results by category ID
         * @query maxPrice Filter results by maximum price
         * @query minPrice Filter results by minimum price
         * @response 200 Search results retrieved successfully
         */
        rateLimit(RateLimitName(RateLimitNames.SEARCH)) {
            get("search") {
                val limit = call.parameters["limit"]?.toIntOrNull() ?: AppConstants.Pagination.DEFAULT_LIMIT
                val name = call.parameters["name"]
                if (name.isNullOrBlank()) {
                    call.respond(
                        ApiResponse.failure("Search term 'name' is required", HttpStatusCode.BadRequest)
                    )
                    return@get
                }
                val queryParams = ProductSearchRequest(
                    limit = limit.coerceAtMost(AppConstants.Pagination.MAX_LIMIT),
                    name = name,
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"]
                )
                call.respond(ApiResponse.success(productController.searchProduct(queryParams), HttpStatusCode.OK))
            }
        }

        /**
         * @tag Product
         * @description Retrieve featured products
         * @operationId getFeaturedProducts
         * @query limit Maximum number of products to return (default 10)
         * @response 200 Featured products retrieved successfully
         */
        get("featured") {
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
            call.respond(
                ApiResponse.success(productController.getFeaturedProducts().take(limit), HttpStatusCode.OK)
            )
        }

        /**
         * @tag Product
         * @description Retrieve best selling products
         * @operationId getBestSellingProducts
         * @query limit Maximum number of products to return (default 10)
         * @response 200 Best selling products retrieved successfully
         */
        get("best-sellers") {
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
            call.respond(
                ApiResponse.success(productController.getBestSellingProducts().take(limit), HttpStatusCode.OK)
            )
        }

        /**
         * @tag Product
         * @description Retrieve hot deal products
         * @operationId getHotDealProducts
         * @query limit Maximum number of products to return (default 10)
         * @response 200 Hot deal products retrieved successfully
         */
        get("hot-deals") {
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
            call.respond(
                ApiResponse.success(productController.getHotDealProducts().take(limit), HttpStatusCode.OK)
            )
        }

        /**
         * @tag Product
         * @description Retrieve products by category
         * @operationId getProductsByCategory
         * @path categoryId (required) Category ID
         * @query limit Maximum number of products to return
         * @response 200 Category products retrieved successfully
         */
        get("category/{categoryId}") {
            val (categoryId) = call.requiredParameters("categoryId") ?: return@get
            val limit = call.parameters["limit"]?.toIntOrNull() ?: AppConstants.Pagination.DEFAULT_LIMIT
            call.respond(
                ApiResponse.success(
                    productController.getProductsByCategory(categoryId).take(limit),
                    HttpStatusCode.OK
                )
            )
        }

        authenticate(RoleManagement.SELLER.role) {

            /**
             * @tag Product
             * @description Retrieve all products belonging to the authenticated seller
             * @operationId getProductsByUser
             * @query limit (required) Maximum number of products to return
             * @query offset Number of products to skip (for pagination)
             * @query maxPrice Filter products by maximum price
             * @query minPrice Filter products by minimum price
             * @query categoryId Filter products by category ID
             * @query subCategoryId Filter products by subcategory ID
             * @query brandId Filter products by brand ID
             * @response 200 Seller's products retrieved successfully
             * @security jwtToken
             */
            get("seller") {
                val limit = call.parameters["limit"]?.toIntOrNull() ?: AppConstants.Pagination.DEFAULT_LIMIT
                val offset = call.parameters["offset"]?.toIntOrNull() ?: AppConstants.Pagination.DEFAULT_OFFSET

                val params = ProductWithFilterRequest(
                    limit = limit.coerceAtMost(AppConstants.Pagination.MAX_LIMIT),
                    offset = offset,
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"]
                )
                call.respond(
                    ApiResponse.success(
                        productController.getProductsByUser(call.currentUserId, params), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Product
             * @description Create a new product listing with all details
             * @operationId createProduct
             * @body ProductRequest Product creation request with all product details
             * @response 200 Product created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ProductRequest>()
                call.respond(
                    ApiResponse.success(
                        productController.createProduct(call.currentUserId, null, requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Product
             * @description Update product details including price, stock, images, and metadata
             * @operationId updateProduct
             * @path id (required) Unique identifier of the product to update
             * @body UpdateProductRequest Product update request with fields to update
             * @response 200 Product updated successfully
             * @security jwtToken
             */
            put("{id}") {
                val (id) = call.requiredParameters("id") ?: return@put
                val requestBody = call.receive<UpdateProductRequest>()
                requestBody.validation()
                call.respond(
                    ApiResponse.success(
                        productController.updateProduct(call.currentUserId, id, requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Product
             * @description Permanently delete a product listing by its ID
             * @operationId deleteProduct
             * @path id (required) Unique identifier of the product to delete
             * @response 200 Product deleted successfully
             * @security jwtToken
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        productController.deleteProduct(call.currentUserId, id), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Product
             * @description Upload an image file for a product
             * @operationId uploadProductImage
             * @form image (required) Image file to upload
             * @response 200 Image uploaded successfully, returns image filename
             * @security jwtToken
             */
            post("image-upload") {
                val multipartData = call.receiveMultipart()
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val originalFileName = part.originalFileName
                            if (originalFileName.isNullOrBlank()) {
                                throw ValidationException("Invalid file name")
                            }

                            // Validate file extension
                            val extension = originalFileName.fileExtension().lowercase()
                            if (extension !in AppConstants.FileUpload.ALLOWED_IMAGE_EXTENSIONS) {
                                throw ValidationException(
                                    "${Message.INVALID_FILE_TYPE}. Allowed: ${AppConstants.FileUpload.ALLOWED_IMAGE_EXTENSIONS.joinToString(", ")}"
                                )
                            }

                            // Validate MIME type if available
                            part.contentType?.let { contentType ->
                                val mimeType = "${contentType.contentType}/${contentSubType(contentType)}"
                                if (mimeType !in AppConstants.FileUpload.ALLOWED_IMAGE_MIME_TYPES) {
                                    throw ValidationException(Message.INVALID_MIME_TYPE)
                                }
                            }

                            // Read file bytes
                            val bytes = withContext(Dispatchers.IO) {
                                part.streamProvider().readBytes()
                            }

                            // Validate file size
                            if (bytes.size > AppConstants.FileUpload.MAX_IMAGE_SIZE) {
                                throw ValidationException("${Message.FILE_TOO_LARGE} (${AppConstants.FileUpload.MAX_IMAGE_SIZE / (1024 * 1024)}MB)")
                            }

                            // Generate secure filename (UUID only, no user input)
                            val imageId = UUID.randomUUID().toString()
                            val safeFileName = "$imageId.$extension"
                            val fileLocation = "${AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION}$safeFileName"

                            // Ensure directory exists
                            val file = File(fileLocation)
                            file.parentFile?.mkdirs()

                            // Write file
                            withContext(Dispatchers.IO) { file.writeBytes(bytes) }

                            call.respond(ApiResponse.success(safeFileName, HttpStatusCode.OK))
                        }
                        else -> Unit
                    }
                }
            }
        }

        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {
            /**
             * @tag Product
             * @description Admin-only: Permanently delete any product by its ID
             * @operationId deleteProductByAdmin
             * @path id (required) Unique identifier of the product to delete
             * @response 200 Product deleted successfully
             * @security jwtToken
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        productController.deleteProductByAdmin(id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}

/**
 * Helper function to extract subtype from ContentType
 */
private fun contentSubType(contentType: ContentType): String {
    return contentType.contentSubtype ?: ""
}
