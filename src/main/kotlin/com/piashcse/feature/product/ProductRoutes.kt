package com.piashcse.feature.product

import com.piashcse.constants.AppConstants
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.requiredParameters
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
    // Main route for product management
    route("product") {

        /**
         * GET request to retrieve product details by product ID.
         *
         * Accessible by customers, sellers, and admins.
         *
         * @tag Product
         * @path id The unique identifier of the product
         * @response 200 Product details retrieved successfully
         */
        get("{id}") {
            val (productId) = call.requiredParameters("id") ?: return@get
            call.respond(ApiResponse.success(productController.getProductDetail(productId), HttpStatusCode.OK))
        }

        /**
         * GET request to retrieve a list of products with optional filters.
         *
         * Accessible by customers, sellers, and admins.
         *
         * @param limit The number of products to retrieve.
         * @param maxPrice Optional maximum price filter.
         * @param minPrice Optional minimum price filter.
         * @param categoryId Optional category filter.
         * @param subCategoryId Optional sub-category filter.
         * @param brandId Optional brand filter.
         */
        get {
            val (limit) = call.requiredParameters("limit") ?: return@get
            val params = ProductWithFilterRequest(
                limit = limit.toInt(),
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"],
                subCategoryId = call.parameters["subCategoryId"],
                brandId = call.parameters["brandId"]
            )
            call.respond(ApiResponse.success(productController.getProducts(params), HttpStatusCode.OK))
        }

        /**
         * GET request to search for products by name with optional filters.
         *
         * Accessible by customers, sellers, and admins.
         *
         * @param limit The number of products to retrieve.
         * @param productName The name of the product to search.
         * @param categoryId Optional category filter.
         * @param maxPrice Optional maximum price filter.
         * @param minPrice Optional minimum price filter.
         */
        get("search") {
            val (limit) = call.requiredParameters("limit") ?: return@get
            val queryParams = ProductSearchRequest(
                limit = limit.toInt(),
                name = call.parameters["name"] ?: return@get,
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"]
            )
            call.respond(ApiResponse.success(productController.searchProduct(queryParams), HttpStatusCode.OK))
        }

        // Routes for sellers to manage their products
        authenticate(RoleManagement.SELLER.role) {

            /**
             * GET request to retrieve seller-specific products with filters.
             *
             * Accessible by sellers.
             *
             * @param limit The number of products to retrieve.
             * @param maxPrice Optional maximum price filter.
             * @param minPrice Optional minimum price filter.
             * @param categoryId Optional category filter.
             * @param subCategoryId Optional sub-category filter.
             * @param brandId Optional brand filter.
             */
            get("seller") {
                val (limit) = call.requiredParameters("limit") ?: return@get
                val params = ProductWithFilterRequest(
                    limit = limit.toInt(),
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"]
                )
                call.respond(
                    ApiResponse.success(
                        productController.getProductById(call.currentUser().userId, params), HttpStatusCode.OK
                    )
                )
            }

            /**
             * POST request to create a new product.
             *
             * Accessible by sellers only.
             *
             * @param requestBody The details of the product to create.
             */
            post {
                val requestBody = call.receive<ProductRequest>()
                call.respond(
                    ApiResponse.success(
                        productController.createProduct(call.currentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to update an existing product.
             *
             * Accessible by sellers only.
             *
             * @param id The ID of the product to update.
             * @param params The parameters to update, including product details.
             */
            put("{id}") {
                val params = UpdateProductRequest(
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"],
                    name = call.parameters["name"],
                    description = call.parameters["description"],
                    stockQuantity = call.parameters["stockQuantity"]?.toInt() ?: 0,
                    price = call.parameters["price"]?.toDoubleOrNull(),
                    discountPrice = call.parameters["discountPrice"]?.toDoubleOrNull(),
                    status = call.parameters["status"],
                    videoLink = call.parameters["videoLink"],
                    hotDeal = call.parameters["hotDeal"]?.toBoolean(),
                    featured = call.parameters["featured"]?.toBoolean(),
                    images = call.parameters["images"]?.split(",")?.toList() ?: emptyList()
                )
                val (id) = call.requiredParameters("id") ?: return@put
                call.respond(
                    ApiResponse.success(
                        productController.updateProduct(call.currentUser().userId, id, params), HttpStatusCode.OK
                    )
                )
            }

            /**
             * DELETE request to delete a product by ID.
             *
             * Accessible by sellers only.
             *
             * @param id The ID of the product to delete.
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        productController.deleteProduct(call.currentUser().userId, id), HttpStatusCode.OK
                    )
                )
            }

            /**
             * POST request to upload a product image.
             *
             * Accessible by sellers only.
             *
             * @param id The ID of the product.
             * @param image The image file to upload.
             */
            post("image-upload") {
                val multipartData = call.receiveMultipart()
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            UUID.randomUUID()?.let { imageId ->
                                val fileName = part.originalFileName as String
                                val fileLocation = fileName.let {
                                    "${AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION}$imageId${it.fileExtension()}"
                                }
                                fileLocation.let {
                                    File(it).writeBytes(withContext(Dispatchers.IO) {
                                        part.streamProvider().readBytes()
                                    })
                                }
                                val fileNameInServer = imageId.toString().plus(fileLocation.fileExtension())
                                call.respond(
                                    ApiResponse.success(fileNameInServer, HttpStatusCode.OK)
                                )
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }

        // Routes for admins to manage all products
        authenticate(RoleManagement.ADMIN.role) {

            /**
             * DELETE request to delete any product by ID (admin only).
             *
             * Accessible by admins.
             *
             * @param id The ID of the product to delete.
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        productController.deleteProduct(call.currentUser().userId, id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}