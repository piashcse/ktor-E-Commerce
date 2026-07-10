package com.piashcse.feature.product

import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public and seller product routes.
 */
fun Route.productRoutes(
    productCatalogService: ProductCatalogService,
    productQueryService: ProductQueryService,
) {
    /**
     * @tag Product
     * @description Retrieve detailed information about a specific product
     */
    get("{id}") {
        val productId = call.requirePathParameter("id")
        call.respond(HttpStatusCode.OK, productCatalogService.getProductDetail(productId))
    }

    /**
     * @tag Product
     * @description Retrieve a paginated list of products with optional filters
     */
    get {
        val (limit, offset) = call.paginateQueryParams(defaultLimit = 10)
        val params =
            ProductWithFilterRequest(
                limit = limit,
                offset = offset,
                maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.request.queryParameters["categoryId"],
                subCategoryId = call.request.queryParameters["subCategoryId"],
                brandId = call.request.queryParameters["brandId"],
                sortBy = call.request.queryParameters["sortBy"],
                sortOrder = call.request.queryParameters["sortOrder"],
            )
        call.respond(HttpStatusCode.OK, productQueryService.getProducts(params))
    }

    /**
     * @tag Product
     * @description Search for products by name
     */
    get("search") {
        val (limit, offset) = call.paginateQueryParams(defaultLimit = 10)
        val queryParams =
            ProductSearchRequest(
                limit = limit,
                offset = offset,
                name = call.requireQueryParameter("name"),
                maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.request.queryParameters["categoryId"],
            )
        call.respond(HttpStatusCode.OK, productQueryService.searchProduct(queryParams))
    }
}

/**
 * Seller product management routes.
 */
fun Route.productSellerRoutes(
    productQueryService: ProductQueryService,
    productCrudService: ProductCrudService,
) {
    /**
     * @tag Product
     * @description Seller: Retrieve seller products
     */
    get {
        val (limit, offset) = call.paginateQueryParams(defaultLimit = 10)
        val params =
            ProductWithFilterRequest(
                limit = limit,
                offset = offset,
                maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                categoryId = call.request.queryParameters["categoryId"],
                subCategoryId = call.request.queryParameters["subCategoryId"],
                brandId = call.request.queryParameters["brandId"],
                sortBy = call.request.queryParameters["sortBy"],
                sortOrder = call.request.queryParameters["sortOrder"],
            )
        call.respond(
            HttpStatusCode.OK,
            productQueryService.getProductsByUser(call.currentUserId, params),
        )
    }

    /**
     * @tag Product
     * @description Seller: Add a new product listing
     */
    post {
        val requestBody = call.receive<ProductRequest>()
        call.respond(
            HttpStatusCode.Created,
            productCrudService.createProduct(call.currentUserId, null, requestBody),
        )
    }

    /**
     * @tag Product
     * @description Seller: Update an existing product listing
     */
    put("{id}") {
        val productId = call.requirePathParameter("id")
        val requestBody = call.receive<UpdateProductRequest>()
        call.respond(
            HttpStatusCode.OK,
            productCrudService.updateProduct(call.currentUserId, productId, requestBody),
        )
    }

    /**
     * @tag Product
     * @description Seller: Permanently delete a product listing
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        val currentUserId = call.currentUserId
        productCrudService.deleteProduct(currentUserId, id)
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

/**
 * Admin product management routes.
 */
fun Route.productAdminRoutes(productCrudService: ProductCrudService) {
    /**
     * @tag Product
     * @description Admin: Permanently delete any product
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        call.respond(
            HttpStatusCode.OK,
            productCrudService.deleteProductAsAdmin(id),
        )
    }
}
