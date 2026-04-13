package com.piashcse.feature.product

import com.piashcse.constants.AppConstants
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
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
            val productId = call.requireParameters("id")
            call.respond(ApiResponse.ok(productController.getProductDetail(productId.first())))
        }

        /**
         * @tag Product
         * @description Retrieve a paginated list of products with optional filters
         * @operationId getProducts
         * @query limit (required) Maximum number of products to return
         * @query maxPrice Filter products by maximum price
         * @query minPrice Filter products by minimum price
         * @query categoryId Filter products by category ID
         * @query subCategoryId Filter products by subcategory ID
         * @query brandId Filter products by brand ID
         * @response 200 Products retrieved successfully
         */
        get {
            val limit = call.requireParameters("limit")
            val params = ProductWithFilterRequest(
                limit = limit.first().toInt(),
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"],
                subCategoryId = call.parameters["subCategoryId"],
                brandId = call.parameters["brandId"]
            )
            call.respond(ApiResponse.ok(productController.getProducts(params)))
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
        get("search") {
            val limit = call.requireParameters("limit")
            val name = call.parameters["name"]
                ?: throw IllegalArgumentException("Missing required parameter: name")
            val queryParams = ProductSearchRequest(
                limit = limit.first().toInt(),
                name = name,
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"]
            )
            call.respond(ApiResponse.ok(productController.searchProduct(queryParams)))
        }

        authenticate(RoleManagement.SELLER.role) {

            /**
             * @tag Product
             * @description Retrieve all products belonging to the authenticated seller
             * @operationId getProductsByUser
             * @query limit (required) Maximum number of products to return
             * @query maxPrice Filter products by maximum price
             * @query minPrice Filter products by minimum price
             * @query categoryId Filter products by category ID
             * @query subCategoryId Filter products by subcategory ID
             * @query brandId Filter products by brand ID
             * @response 200 Seller's products retrieved successfully
             * @security jwtToken
             */
            get("seller") {
                val limit = call.requireParameters("limit")
                val params = ProductWithFilterRequest(
                    limit = limit.first().toInt(),
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"]
                )
                call.respond(
                    ApiResponse.ok(
                        productController.getProductsByUser(call.currentUserId, params)
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
                    ApiResponse.ok(
                        productController.createProduct(call.currentUserId, null, requestBody)
                    )
                )
            }

            /**
             * @tag Product
             * @description Update product details including price, stock, images, and metadata
             * @operationId updateProduct
             * @path id (required) Unique identifier of the product to update
             * @query categoryId Category ID to assign the product to
             * @query subCategoryId Subcategory ID to assign the product to
             * @query brandId Brand ID to assign the product to
             * @query name Product name
             * @query description Product description
             * @query stockQuantity Current stock quantity
             * @query price Product price
             * @query discountPrice Discounted price
             * @query status Product status (active, inactive, etc.)
             * @query videoLink Product video URL
             * @query hotDeal Mark as hot deal
             * @query featured Mark as featured product
             * @query images Comma-separated list of image URLs
             * @response 200 Product updated successfully
             * @security jwtToken
             */
            put("{id}") {
                val params = UpdateProductRequest(
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"],
                    name = call.parameters["name"],
                    description = call.parameters["description"],
                    stockQuantity = call.parameters["stockQuantity"]?.toInt(),
                    price = call.parameters["price"]?.toDoubleOrNull(),
                    discountPrice = call.parameters["discountPrice"]?.toDoubleOrNull(),
                    status = call.parameters["status"],
                    videoLink = call.parameters["videoLink"],
                    hotDeal = call.parameters["hotDeal"]?.toBoolean(),
                    featured = call.parameters["featured"]?.toBoolean(),
                    freeShipping = call.parameters["freeShipping"]?.toBoolean(),
                    images = call.parameters["images"]?.split(",")?.toList() ?: emptyList()
                )
                val id = call.requireParameters("id")
                call.respond(
                    ApiResponse.ok(
                        productController.updateProduct(call.currentUserId, id.first(), params)
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
                val id = call.requireParameters("id")
                val currentUserId = call.currentUserId
                val userType = call.principal<com.piashcse.model.request.JwtTokenRequest>()?.userType
                if (userType == "ADMIN" || userType == "SUPER_ADMIN") {
                    productController.deleteProductAsAdmin(id.first())
                } else {
                    productController.deleteProduct(currentUserId, id.first())
                }
                call.respond(ApiResponse.okMessage("Product deleted successfully"))
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
                var uploadedFileName: String? = null

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val fileName = part.originalFileName
                            if (fileName.isNullOrBlank()) {
                                throw IllegalArgumentException("File name is required")
                            }

                            val extension = fileName.substringAfterLast('.', "").lowercase()
                            val allowedExtensions = listOf("jpg", "jpeg", "png", "webp", "gif")
                            if (extension !in allowedExtensions) {
                                throw IllegalArgumentException("Invalid file type. Allowed: ${allowedExtensions.joinToString(", ")}")
                            }

                            UUID.randomUUID()?.let { imageId ->
                                val fileLocation = "${AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION}$imageId.$extension"
                                File(fileLocation).writeBytes(withContext(Dispatchers.IO) {
                                    part.streamProvider().readBytes()
                                })
                                uploadedFileName = "$imageId.$extension"
                                call.respond(
                                    HttpStatusCode.OK, ApiResponse.ok(uploadedFileName)
                                )
                            }
                        }

                        else -> Unit
                    }
                    part.dispose()
                }

                if (uploadedFileName == null) {
                    throw IllegalArgumentException("No file uploaded")
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
            delete("/admin/{id}") {
                val id = call.requireParameters("id")
                call.respond(
                    ApiResponse.ok(
                        productController.deleteProduct(call.currentUserId, id.first())
                    )
                )
            }
        }
    }
}