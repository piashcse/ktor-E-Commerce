package com.piashcse.feature.product

import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
import com.piashcse.service.UploadService
import com.piashcse.utils.validator.ValidationException
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
 * Public and seller product routes.
 */
fun Route.productRoutes(productController: ProductService) {
    /**
     * @tag Product
     * @description Retrieve detailed information about a specific product
     */
    get("{id}") {
        val productId = call.requireParameters("id")
        call.respond(HttpStatusCode.OK, productController.getProductDetail(productId.first()))
    }

    /**
     * @tag Product
     * @description Retrieve a paginated list of products with optional filters
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
     * @description Search for products by name
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
         * @description Seller: Retrieve seller products
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
         * @description Seller: Add a new product listing
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
         * @description Seller: Update an existing product listing
         */
        put("{id}") {
            val productId = call.requireParameters("id").first()
            val requestBody = call.receive<UpdateProductRequest>()
            call.respond(
                HttpStatusCode.OK,
                productController.updateProduct(call.currentUserId, productId, requestBody)
            )
        }

        /**
         * @tag Product
         * @description Seller: Permanently delete a product listing
         */
        delete("{id}") {
            val id = call.requireParameters("id")
            val currentUserId = call.currentUserId
            productController.deleteProduct(currentUserId, id.first())
            call.respond(HttpStatusCode.OK, mapOf("message" to "Product deleted successfully"))
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

            call.respond(HttpStatusCode.OK, imageUrl ?: throw ValidationException("No file uploaded"))
        }
    }
}

/**
 * Admin product management routes.
 */
fun Route.productAdminRoutes(productController: ProductService) {
    adminAuth {
        /**
         * @tag Product
         * @description Admin: Permanently delete any product
         */
        delete("{id}") {
            val id = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                productController.deleteProductAsAdmin(id.first())
            )
        }
    }
}
