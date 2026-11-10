package com.piashcse.feature.product

import com.piashcse.constants.Message
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.searchRateLimit
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.service.CacheService
import com.piashcse.service.UploadService
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public and seller product routes.
 */
fun Route.productRoutes() {
    val productCatalogService: ProductCatalogService by inject()
    val productRepo: ProductRepository by inject()
    val variantService: ProductVariantService by inject()

    searchRateLimit {
        /**
         * @tag Product
         * @description Retrieve detailed information about a specific product
         */
        get("{id}") {
            val productId = call.requirePathParameter("id")
            productRepo.incrementViewCount(productId)
            CacheService.cache.invalidate("products:detail:$productId")
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
         * @description List purchasable variants for a product
         */
        get("{id}/variants") {
            call.respondOk(variantService.listVariants(call.requirePathParameter("id")))
        }

        /**
         * @tag Product
         * @description List option dimensions and allowed values for a product
         */
        get("{id}/options") {
            call.respondOk(variantService.listOptions(call.requirePathParameter("id")))
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
    val variantService: ProductVariantService by inject()
    /**
     * @tag Product
     * @description Seller: Retrieve seller products
     */
    get {
        call.respondOk(productRepo.getProductsByUser(call.currentUserId, call.productWithFilterRequest(defaultPerPage = 10)))
    }

    sellerWriteRateLimit {
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
            call.respondOk(MessageResponse("Product deleted successfully"))
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
                    // Size cap (5 MB) + MIME allowlist enforced before the upload call.
                    val bytes = UploadService.readAndValidateImagePart(part, "product image")
                    val fileName = UploadService.uploadProductImage(part, bytes)
                    imageUrl = UploadService.getProductImageUrl(fileName)
                }
                part.dispose()
            }

            call.respondOk(imageUrl ?: throw ValidationException(Message.Validation.FILE_REQUIRED))
        }

        /**
         * @tag Product
         * @description Seller: Create a product variant
         */
        post("{id}/variants") {
            val productId = call.requirePathParameter("id")
            call.respondCreated(
                variantService.createVariant(call.currentUserId, productId, call.receive<ProductVariantRequest>()),
            )
        }

        /**
         * @tag Product
         * @description Seller: Update a product variant
         */
        put("{id}/variants/{variantId}") {
            val productId = call.requirePathParameter("id")
            val variantId = call.requirePathParameter("variantId")
            call.respondOk(
                variantService.updateVariant(
                    call.currentUserId,
                    productId,
                    variantId,
                    call.receive<ProductVariantRequest>(),
                ),
            )
        }

        /**
         * @tag Product
         * @description Seller: Delete a product variant
         */
        delete("{id}/variants/{variantId}") {
            variantService.deleteVariant(
                call.currentUserId,
                call.requirePathParameter("id"),
                call.requirePathParameter("variantId"),
            )
            call.respondOk(MessageResponse("Variant deleted successfully"))
        }

        /**
         * @tag Product
         * @description Seller: Replace the option values attached to a variant
         */
        put("{id}/variants/{variantId}/options") {
            val productId = call.requirePathParameter("id")
            val variantId = call.requirePathParameter("variantId")
            call.respondOk(
                variantService.setVariantOptions(
                    call.currentUserId,
                    productId,
                    variantId,
                    call.receive<SetVariantOptionsRequest>(),
                ),
            )
        }

        /**
         * @tag Product
         * @description Seller: Create an option dimension (e.g. Size)
         */
        post("{id}/options") {
            val productId = call.requirePathParameter("id")
            call.respondCreated(
                variantService.createOption(call.currentUserId, productId, call.receive<ProductOptionRequest>()),
            )
        }

        /**
         * @tag Product
         * @description Seller: Delete an option dimension and its values
         */
        delete("{id}/options/{optionId}") {
            variantService.deleteOption(
                call.currentUserId,
                call.requirePathParameter("id"),
                call.requirePathParameter("optionId"),
            )
            call.respondOk(MessageResponse("Option deleted successfully"))
        }

        /**
         * @tag Product
         * @description Seller: Add a value to an option (e.g. M)
         */
        post("{id}/options/{optionId}/values") {
            val productId = call.requirePathParameter("id")
            val optionId = call.requirePathParameter("optionId")
            call.respondCreated(
                variantService.addOptionValue(
                    call.currentUserId,
                    productId,
                    optionId,
                    call.receive<ProductOptionValueRequest>(),
                ),
            )
        }

        /**
         * @tag Product
         * @description Seller: Delete an option value
         */
        delete("{id}/options/{optionId}/values/{valueId}") {
            variantService.deleteOptionValue(
                call.currentUserId,
                call.requirePathParameter("id"),
                call.requirePathParameter("optionId"),
                call.requirePathParameter("valueId"),
            )
            call.respondOk(MessageResponse("Option value deleted successfully"))
        }
    }
}

/**
 * Admin product management routes.
 */
fun Route.productAdminRoutes() {
    val productCrudService: ProductCrudService by inject()
    adminWriteRateLimit {
        /**
         * @tag Product
         * @description Admin: Permanently delete any product
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            productCrudService.deleteProductAsAdmin(id)
            call.respondOk(MessageResponse("Product deleted successfully"))
        }
    }
}
