package com.piashcse.feature.product

import com.piashcse.constants.Message
import com.piashcse.feature.recommendation.RecommendationResponse
import com.piashcse.feature.recommendation.RecommendationService
import com.piashcse.feature.recommendation.recommendationLimit
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.ProductOptionResponse
import com.piashcse.model.response.ProductOptionValueResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.ProductVariantResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.docs
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
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
    val recommendationService: RecommendationService by inject()

    searchRateLimit {
        /**
         * Public best-seller recommendations (guest-safe fallback)
         *
         * @tag Product
         * @description Public best-seller recommendations (guest-safe fallback)
         */
        get("recommendations") {
            call.respondOk(recommendationService.bestSellers(call.recommendationLimit()))
        }.docs("Best-seller recommendations", "Public, guest-safe fallback ranked by sales and rating") {
            responses { ok<RecommendationResponse>("Ranked products") }
        }

        /**
         * Retrieve detailed information about a specific product
         *
         * @tag Product
         * @description Retrieve detailed information about a specific product
         */
        get("{id}") {
            val productId = call.requirePathParameter("id")
            productRepo.incrementViewCount(productId)
            CacheService.cache.invalidate("products:detail:$productId")
            call.respondOk(productCatalogService.getProductDetail(productId))
        }.docs("Get product detail", "Includes images and purchasable variants") {
            responses { ok<ProductResponse>("Product detail") }
        }

        /**
         * Related products from the same category/subcategory
         *
         * @tag Product
         * @description Related products from the same category/subcategory
         */
        get("{id}/related") {
            call.respondOk(
                recommendationService.related(
                    call.requirePathParameter("id"),
                    call.recommendationLimit(),
                ),
            )
        }.docs("Related products", "Same category, subcategory boosted, ranked by sales and rating") {
            responses { ok<RecommendationResponse>("Related products") }
        }

        /**
         * Products frequently purchased with this product
         *
         * @tag Product
         * @description Products frequently purchased with this product
         */
        get("{id}/also-bought") {
            call.respondOk(
                recommendationService.alsoBought(
                    call.requirePathParameter("id"),
                    call.recommendationLimit(),
                ),
            )
        }.docs("Frequently bought together", "Co-purchase counts from paid, non-canceled orders") {
            responses { ok<RecommendationResponse>("Co-purchased products") }
        }

        /**
         * Retrieve a paginated list of products with optional filters
         *
         * @tag Product
         * @description Retrieve a paginated list of products with optional filters
         */
        get {
            call.respondOk(productRepo.getProducts(call.productWithFilterRequest(defaultPerPage = 10)))
        }

        /**
         * List purchasable variants for a product
         *
         * @tag Product
         * @description List purchasable variants for a product
         */
        get("{id}/variants") {
            call.respondOk(variantService.listVariants(call.requirePathParameter("id")))
        }.docs("List product variants", "Purchasable variants with option values") {
            responses { ok<List<ProductVariantResponse>>("Variants") }
        }

        /**
         * List option dimensions and allowed values for a product
         *
         * @tag Product
         * @description List option dimensions and allowed values for a product
         */
        get("{id}/options") {
            call.respondOk(variantService.listOptions(call.requirePathParameter("id")))
        }.docs("List product options", "Option dimensions (for example Size, Color) and allowed values") {
            responses { ok<List<ProductOptionResponse>>("Options") }
        }

        /**
         * Search for products by name with fuzzy matching, faceted aggregation, and ranking
         *
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
     * Seller: Retrieve seller products
     *
     * @tag Product
     * @description Seller: Retrieve seller products
     */
    get {
        call.respondOk(productRepo.getProductsByUser(call.currentUserId, call.productWithFilterRequest(defaultPerPage = 10)))
    }

    sellerWriteRateLimit {
        /**
         * Seller: Add a new product listing
         *
         * @tag Product
         * @description Seller: Add a new product listing
         */
        post {
            call.respondCreated(productCrudService.createProduct(call.currentUserId, null, call.receive<ProductRequest>()))
        }.docs("Create product") {
            requestBody { jsonBody<ProductRequest>() }
            responses { created<ProductResponse>("Product created") }
        }

        /**
         * Seller: Update an existing product listing
         *
         * @tag Product
         * @description Seller: Update an existing product listing
         */
        put("{id}") {
            val productId = call.requirePathParameter("id")
            call.respondOk(productCrudService.updateProduct(call.currentUserId, productId, call.receive<UpdateProductRequest>()))
        }.docs("Update product") {
            requestBody { jsonBody<UpdateProductRequest>() }
            responses { ok<ProductResponse>("Product updated") }
        }

        /**
         * Seller: Permanently delete a product listing
         *
         * @tag Product
         * @description Seller: Permanently delete a product listing
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            productCrudService.deleteProduct(call.currentUserId, id)
            call.respondOk(MessageResponse("Product deleted successfully"))
        }.docs("Delete product", "Soft-deletes the listing and preserves order history")

        /**
         * Seller: Upload a product image
         *
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
         * Seller: Create a product variant
         *
         * @tag Product
         * @description Seller: Create a product variant
         */
        post("{id}/variants") {
            val productId = call.requirePathParameter("id")
            call.respondCreated(
                variantService.createVariant(call.currentUserId, productId, call.receive<ProductVariantRequest>()),
            )
        }.docs("Create product variant") {
            requestBody { jsonBody<ProductVariantRequest>() }
            responses { created<ProductVariantResponse>("Variant created") }
        }

        /**
         * Seller: Update a product variant
         *
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
        }.docs("Update product variant") {
            requestBody { jsonBody<ProductVariantRequest>() }
            responses { ok<ProductVariantResponse>("Variant updated") }
        }

        /**
         * Seller: Delete a product variant
         *
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
        }.docs("Delete product variant", "Promotes another variant as default when needed")

        /**
         * Seller: Replace the option values attached to a variant
         *
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
        }.docs("Attach option values to a variant", "One value per option") {
            requestBody { jsonBody<SetVariantOptionsRequest>() }
            responses { ok<ProductVariantResponse>("Variant options replaced") }
        }

        /**
         * Seller: Create an option dimension (e.g. Size)
         *
         * @tag Product
         * @description Seller: Create an option dimension (e.g. Size)
         */
        post("{id}/options") {
            val productId = call.requirePathParameter("id")
            call.respondCreated(
                variantService.createOption(call.currentUserId, productId, call.receive<ProductOptionRequest>()),
            )
        }.docs("Create option dimension") {
            requestBody { jsonBody<ProductOptionRequest>() }
            responses { created<ProductOptionResponse>("Option created") }
        }

        /**
         * Seller: Delete an option dimension and its values
         *
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
        }.docs("Delete option dimension", "Also removes its values and variant links")

        /**
         * Seller: Add a value to an option (e.g. M)
         *
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
        }.docs("Add option value") {
            requestBody { jsonBody<ProductOptionValueRequest>() }
            responses { created<ProductOptionValueResponse>("Option value created") }
        }

        /**
         * Seller: Delete an option value
         *
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
        }.docs("Delete option value")
    }
}

/**
 * Admin product management routes.
 */
fun Route.productAdminRoutes() {
    val productCrudService: ProductCrudService by inject()
    adminWriteRateLimit {
        /**
         * Admin: Permanently delete any product
         *
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
