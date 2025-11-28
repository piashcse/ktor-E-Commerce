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
    route("/product") {

        /**
         * @tag Product
         * @path id (required)
         * @response 200 [Response]
         */
        get("{id}") {
            val (productId) = call.requiredParameters("id") ?: return@get
            call.respond(ApiResponse.success(productController.getProductDetail(productId), HttpStatusCode.OK))
        }

        /**
         * @tag Product
         * @query limit (required)
         * @query maxPrice
         * @query minPrice
         * @query categoryId
         * @query subCategoryId
         * @query brandId
         * @response 200 [Response]
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
         * @tag Product
         * @query limit (required)
         * @query name (required)
         * @query categoryId
         * @query maxPrice
         * @query minPrice
         * @response 200 [Response]
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
             * @tag Product
             * @query limit (required)
             * @query maxPrice
             * @query minPrice
             * @query categoryId
             * @query subCategoryId
             * @query brandId
             * @response 200 [Response]
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
                        productController.getProductsByUser(call.currentUser().userId, params), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Product
             * @body requestBody
             */
            post {
                val requestBody = call.receive<ProductRequest>()
                call.respond(
                    ApiResponse.success(
                        productController.createProduct(call.currentUser().userId, null, requestBody), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Product
             * @path id (required)
             * @query categoryId
             * @query subCategoryId
             * @query brandId
             * @query name
             * @query description
             * @query stockQuantity
             * @query price
             * @query discountPrice
             * @query status
             * @query videoLink
             * @query hotDeal
             * @query featured
             * @query images
             * @response 200 [Response]
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
                    freeShipping = call.parameters["freeShipping"]?.toBoolean(),
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
             * @tag Product
             * @path id (required)
             * @response 200 [Response]
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
             * @tag Product
             * @param id
             * @param image
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

        // Routes for admins and super admins to manage all products
        authenticate(RoleManagement.ADMIN.role, RoleManagement.SUPER_ADMIN.role) {

            /**
             * @tag Product
             * @path id (required)
             * @response 200 [Response]
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