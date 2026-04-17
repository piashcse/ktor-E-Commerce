package com.piashcse.feature.product

import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
import com.piashcse.service.UploadService
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing products. Different routes are available based on user roles (CUSTOMER, SELLER, ADMIN).
 *
 * @param productController The controller handling product-related operations.
 */
fun Route.productRoutes(productController: ProductService) {

        /**
         * @tag Product
         * @description Retrieve detailed information about a specific product
         * @operationId getProductDetail
         * @path id (required) Unique identifier of the product
         * @response 200 Product details retrieved successfully
         */
        get("{id}") {
            val productId = call.requireParameters("id")
            call.respond(HttpStatusCode.OK, productController.getProductDetail(productId.first()))
        }

        /**
         * @tag Product
         * @description Retrieve a paginated list of products with optional filters
         * @operationId getProducts
         * @query limit Maximum number of products to return (default 10)
         * @query offset Number of products to skip (default 0)
         * @query maxPrice Filter products by maximum price
         * @query minPrice Filter products by minimum price
         * @query categoryId Filter products by category ID
         * @query subCategoryId Filter products by subcategory ID
         * @query brandId Filter products by brand ID
         * @response 200 Products retrieved successfully
         */
        get {
            val (limit, offset) = call.paginationParameters(defaultLimit = 10)
            val params = ProductWithFilterRequest(
                limit = limit,
                offset = offset,
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"],
                subCategoryId = call.parameters["subCategoryId"],
                brandId = call.parameters["brandId"]
            )
            params.validation()
            call.respond(HttpStatusCode.OK, productController.getProducts(params))
        }

        /**
         * @tag Product
         * @description Search for products by name with pagination and filters
         * @operationId searchProduct
         * @query name (required) Product name search query
         * @query limit Maximum number of products to return (default 10)
         * @query offset Number of products to skip (default 0)
         * @query maxPrice Filter products by maximum price
         * @query minPrice Filter products by minimum price
         * @query categoryId Filter products by category ID
         * @response 200 Search results retrieved successfully
         */
        get("search") {
            val (limit, offset) = call.paginationParameters(defaultLimit = 10)
            val queryParams = ProductSearchRequest(
                limit = limit,
                offset = offset,
                name = call.requireParameters("name").first(),
                maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.parameters["categoryId"]
            )
            queryParams.validation()
            call.respond(HttpStatusCode.OK, productController.searchProduct(queryParams))
        }

        sellerAuth {
            /**
             * @tag Product
             * @description Retrieve a paginated list of products belonging to the authenticated seller
             * @operationId getSellerProducts
             * @query limit Maximum number of products to return (default 10)
             * @query offset Number of products to skip (default 0)
             * @query maxPrice Filter products by maximum price
             * @query minPrice Filter products by minimum price
             * @query categoryId Filter products by category ID
             * @query subCategoryId Filter products by subcategory ID
             * @query brandId Filter products by brand ID
             * @response 200 Seller products retrieved successfully
             * @security jwtToken
             */
            get("seller") {
                val (limit, offset) = call.paginationParameters(defaultLimit = 10)
                val params = ProductWithFilterRequest(
                    limit = limit,
                    offset = offset,
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"]
                )
                params.validation()
                call.respond(
                    HttpStatusCode.OK,
                    productController.getProductsByUser(call.currentUserId, params)
                )
            }

            /**
             * @tag Product
             * @description Add a new product listing for the authenticated seller
             * @operationId createProduct
             * @body ProductRequest Product creation details
             * @response 200 Product created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<ProductRequest>()
                requestBody.validation()
                call.respond(
                    HttpStatusCode.OK,
                    productController.createProduct(call.currentUserId, null, requestBody)
                )
            }

            /**
             * @tag Product
             * @description Update an existing product listing for the seller
             * @operationId updateProduct
             * @path id (required) Unique identifier of the product to update
             * @body UpdateProductRequest Product update details
             * @response 200 Product updated successfully
             * @security jwtToken
             */
            put("{id}") {
                val productId = call.requireParameters("id").first()
                val requestBody = call.receive<UpdateProductRequest>()
                // requestBody.validation() // Add this when UpdateProductRequest has validation
                call.respond(
                    HttpStatusCode.OK,
                    productController.updateProduct(call.currentUserId, productId, requestBody)
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
                call.respond(HttpStatusCode.OK, mapOf("message" to "Product deleted successfully"))
            }

            /**
             * @tag Product
             * @description Upload a product image (JPG, PNG, WebP, GIF - max 10MB)
             * @operationId uploadProductImage
             * @form image (required) Product image file
             * @response 200 Returns image URL
             * @security jwtToken
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

                call.respond(HttpStatusCode.OK, imageUrl ?: throw ValidationException("No file uploaded"))
            }
        }

        adminAuth {
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
                    HttpStatusCode.OK,
                    productController.deleteProduct(call.currentUserId, id.first())
                )
            }
        }
    }
