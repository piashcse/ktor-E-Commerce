package com.piashcse.modules.product.routes

import com.piashcse.modules.product.controller.ProductController
import com.piashcse.database.models.product.request.ProductRequest
import com.piashcse.database.models.product.request.ProductSearchRequest
import com.piashcse.database.models.product.request.ProductWithFilterRequest
import com.piashcse.database.models.product.request.UpdateProduct
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.productRoutes(productController: ProductController) {
    // Main route for product management
    route("product") {

        /**
         * GET request to retrieve product details by product ID.
         *
         * Accessible by customers, sellers, and admins.
         *
         * @param id The unique identifier of the product.
         */
        get("{id}", {
            tags("Product")
            request {
                pathParameter<String>("id") {
                    required = true
                }
            }
            apiResponse()
        }) {
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
        get({
            tags("Product")
            request {
                queryParameter<Int>("limit") {
                    required = true
                }
                queryParameter<Double>("maxPrice")
                queryParameter<Double>("minPrice")
                queryParameter<String>("categoryId")
                queryParameter<String>("subCategoryId")
                queryParameter<String>("brandId")
            }
            apiResponse()
        }) {
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
        get("search", {
            tags("Product")
            request {
                queryParameter<Int>("limit") {
                    required = true
                }
                queryParameter<String>("name") {
                    required = true
                }
                queryParameter<String>("categoryId")
                queryParameter<Double>("maxPrice")
                queryParameter<Double>("minPrice")
            }
            apiResponse()
        }) {
            val (limit) = call.requiredParameters("limit") ?: return@get
            val queryParams = ProductSearchRequest(
                limit = limit.toInt(),
                name = call.parameters["name"]!!,
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
            get("seller", {
                tags("Product")
                summary = "auth[seller]"
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                    queryParameter<Double>("maxPrice")
                    queryParameter<Double>("minPrice")
                    queryParameter<String>("categoryId")
                    queryParameter<String>("subCategoryId")
                    queryParameter<String>("brandId")
                }
                apiResponse()
            }) {
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
            post({
                tags("Product")
                summary = "auth[seller]"
                request {
                    body<ProductRequest>()
                }
                apiResponse()
            }) {
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
            put("{id}", {
                tags("Product")
                summary = "auth[seller]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("categoryId")
                    queryParameter<String>("subCategoryId")
                    queryParameter<String>("brandId")
                    queryParameter<String>("name")
                    queryParameter<String>("description")
                    queryParameter<Int>("stockQuantity")
                    queryParameter<Long>("price")
                    queryParameter<Long>("discountPrice")
                    queryParameter<String>("status")
                    queryParameter<String>("videoLink")
                    queryParameter<String>("hotDeal")
                    queryParameter<String>("featured")
                    queryParameter<Array<String>>("images")
                }
                apiResponse()
            }) {
                val params = UpdateProduct(
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
                    hotDeal = call.parameters["hotDeal"].toBoolean(),
                    featured = call.parameters["featured"].toBoolean(),
                    images = call.parameters["images"]?.split(",") ?: arrayListOf()
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
            delete("{id}", {
                tags("Product")
                summary = "auth[seller]"
                request {
                    pathParameter<String>("id")
                }
                apiResponse()
            }) {
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
            post("image-upload", {
                tags("Product")
                summary = "auth[seller]"
                request {
                    multipartBody {
                        mediaTypes = setOf(ContentType.MultiPart.FormData)
                        part<File>("image") {
                            mediaTypes = setOf(
                                ContentType.Image.PNG, ContentType.Image.JPEG, ContentType.Image.SVG
                            )
                        }
                    }
                }
                apiResponse()
            }) {
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
            delete("{id}", {
                tags("Product")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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