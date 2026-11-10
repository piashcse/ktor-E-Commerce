@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.product

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
import com.piashcse.model.response.SearchSuggestionsResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.searchRateLimit
import com.piashcse.plugin.sellerWriteRateLimit
import com.piashcse.plugin.standardErrors
import com.piashcse.service.CacheService
import com.piashcse.service.UploadService
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
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
         */
        get("recommendations") {
            call.respondOk(recommendationService.bestSellers(call.recommendationLimit()))
        }.describe {
            summary = "Best-seller recommendations"
            description = "Public, guest-safe fallback ranked by sales and rating"
            responses {
                ok<RecommendationResponse>("Ranked products")
                standardErrors()
            }
        }

        /**
         * Public autocomplete suggestions for products, categories and brands
         */
        get("suggest") {
            val q = call.request.queryParameters["q"].orEmpty()
            val limit = (call.queryInt("limit") ?: 5).coerceIn(1, 10)
            call.respondOk(productRepo.getSuggestions(q, limit))
        }.describe {
            summary = "Product search suggestions"
            description = "Autocomplete over product names, categories and brands"
            parameters {
                query("q") { description = "Prefix or substring to match" }
                query("limit") { description = "1..10 (default 5)" }
            }
            responses { ok<SearchSuggestionsResponse>("Suggestions") }
        }

        /**
         * Retrieve detailed information about a specific product
         */
        get("{id}") {
            val productId = call.requirePathParameter("id")
            productRepo.incrementViewCount(productId)
            CacheService.cache.invalidate("products:detail:$productId")
            call.respondOk(productCatalogService.getProductDetail(productId))
        }.describe {
            summary = "Get product detail"
            description = "Includes images and purchasable variants"
            responses {
                ok<ProductResponse>("Product detail")
                standardErrors()
            }
        }

        /**
         * Related products from the same category/subcategory
         */
        get("{id}/related") {
            call.respondOk(
                recommendationService.related(
                    call.requirePathParameter("id"),
                    call.recommendationLimit(),
                ),
            )
        }.describe {
            summary = "Related products"
            description = "Same category, subcategory boosted, ranked by sales and rating"
            responses {
                ok<RecommendationResponse>("Related products")
                standardErrors()
            }
        }

        /**
         * Products frequently purchased with this product
         */
        get("{id}/also-bought") {
            call.respondOk(
                recommendationService.alsoBought(
                    call.requirePathParameter("id"),
                    call.recommendationLimit(),
                ),
            )
        }.describe {
            summary = "Frequently bought together"
            description = "Co-purchase counts from paid, non-canceled orders"
            responses {
                ok<RecommendationResponse>("Co-purchased products")
                standardErrors()
            }
        }

        /**
         * Retrieve a paginated list of products with optional filters
         */
        get {
            call.respondOk(productRepo.getProducts(call.productWithFilterRequest(defaultPerPage = 10)))
        }.describe { summary = "Retrieve a paginated list of products with optional filters" }

        /**
         * List purchasable variants for a product
         */
        get("{id}/variants") {
            call.respondOk(variantService.listVariants(call.requirePathParameter("id")))
        }.describe {
            summary = "List product variants"
            description = "Purchasable variants with option values"
            responses { ok<List<ProductVariantResponse>>("Variants") }
        }

        /**
         * List option dimensions and allowed values for a product
         */
        get("{id}/options") {
            call.respondOk(variantService.listOptions(call.requirePathParameter("id")))
        }.describe {
            summary = "List product options"
            description = "Option dimensions (for example Size, Color) and allowed values"
            responses { ok<List<ProductOptionResponse>>("Options") }
        }

        /**
         * Search for products by name with fuzzy matching, faceted aggregation, and ranking
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
        }.describe { summary = "Search for products by name with fuzzy matching, faceted aggregation, and ranking" }
    }
}

/**
 * Seller product management routes.
 */
fun Route.productSellerRoutes() {
    val productRepo: ProductRepository by inject()
    val productCrudService: ProductCrudService by inject()
    /**
     * Seller - Retrieve seller products
     *
     */
    get {
        call.respondOk(productRepo.getProductsByUser(call.currentUserId, call.productWithFilterRequest(defaultPerPage = 10)))
    }.describe { summary = "Seller - Retrieve seller products" }

    sellerWriteRateLimit {
        /**
         * Seller - Add a new product listing
         *
         */
        post {
            call.respondCreated(productCrudService.createProduct(call.currentUserId, null, call.receive<ProductRequest>()))
        }.describe {
            summary = "Create product"
            requestBody { jsonBody<ProductRequest>() }
            responses {
                created<ProductResponse>("Product created")
                standardErrors()
            }
        }

        /**
         * Seller - Update an existing product listing
         *
         */
        put("{id}") {
            val productId = call.requirePathParameter("id")
            call.respondOk(productCrudService.updateProduct(call.currentUserId, productId, call.receive<UpdateProductRequest>()))
        }.describe {
            summary = "Update product"
            requestBody { jsonBody<UpdateProductRequest>() }
            responses {
                ok<ProductResponse>("Product updated")
                standardErrors()
            }
        }

        /**
         * Seller - Permanently delete a product listing
         *
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            productCrudService.deleteProduct(call.currentUserId, id)
            call.respondOk(MessageResponse("Product deleted successfully"))
        }.describe {
            summary = "Delete product"
            description = "Soft-deletes the listing and preserves order history"
        }

        /**
         * Seller - Upload a product image
         *
         */
        post("image-upload") {
            val imageUrl =
                call.receiveSingleImage(
                    purpose = "product image",
                    upload = UploadService::uploadProductImage,
                    publicUrl = UploadService::getProductImageUrl,
                )
            call.respondOk(imageUrl)
        }.describe { summary = "Seller - Upload a product image" }

        variantSellerRoutes()
    }
}

/**
 * Admin product management routes.
 */
fun Route.productAdminRoutes() {
    val productCrudService: ProductCrudService by inject()
    adminWriteRateLimit {
        /**
         * Admin - Permanently delete any product
         *
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            productCrudService.deleteProductAsAdmin(id)
            call.respondOk(MessageResponse("Product deleted successfully"))
        }.describe { summary = "Admin - Permanently delete any product" }
    }
}

/** Seller variant/option management, mounted inside the product write rate limit. */
private fun Route.variantSellerRoutes() {
    val variantService: ProductVariantService by inject()
    /**
     * Seller - Create a product variant
     *
     */
    post("{id}/variants") {
        val productId = call.requirePathParameter("id")
        call.respondCreated(
            variantService.createVariant(call.currentUserId, productId, call.receive<ProductVariantRequest>()),
        )
    }.describe {
        summary = "Create product variant"
        requestBody { jsonBody<ProductVariantRequest>() }
        responses {
            created<ProductVariantResponse>("Variant created")
            standardErrors()
        }
    }

    /**
     * Seller - Update a product variant
     *
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
    }.describe {
        summary = "Update product variant"
        requestBody { jsonBody<ProductVariantRequest>() }
        responses {
            ok<ProductVariantResponse>("Variant updated")
            standardErrors()
        }
    }

    /**
     * Seller - Delete a product variant
     *
     */
    delete("{id}/variants/{variantId}") {
        variantService.deleteVariant(
            call.currentUserId,
            call.requirePathParameter("id"),
            call.requirePathParameter("variantId"),
        )
        call.respondOk(MessageResponse("Variant deleted successfully"))
    }.describe {
        summary = "Delete product variant"
        description = "Promotes another variant as default when needed"
    }

    /**
     * Seller - Replace the option values attached to a variant
     *
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
    }.describe {
        summary = "Attach option values to a variant"
        description = "One value per option"
        requestBody { jsonBody<SetVariantOptionsRequest>() }
        responses {
            ok<ProductVariantResponse>("Variant options replaced")
            standardErrors()
        }
    }

    /**
     * Seller - Create an option dimension (e.g. Size)
     *
     */
    post("{id}/options") {
        val productId = call.requirePathParameter("id")
        call.respondCreated(
            variantService.createOption(call.currentUserId, productId, call.receive<ProductOptionRequest>()),
        )
    }.describe {
        summary = "Create option dimension"
        requestBody { jsonBody<ProductOptionRequest>() }
        responses {
            created<ProductOptionResponse>("Option created")
            standardErrors()
        }
    }

    /**
     * Seller - Delete an option dimension and its values
     *
     */
    delete("{id}/options/{optionId}") {
        variantService.deleteOption(
            call.currentUserId,
            call.requirePathParameter("id"),
            call.requirePathParameter("optionId"),
        )
        call.respondOk(MessageResponse("Option deleted successfully"))
    }.describe {
        summary = "Delete option dimension"
        description = "Also removes its values and variant links"
    }

    /**
     * Seller - Add a value to an option (e.g. M)
     *
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
    }.describe {
        summary = "Add option value"
        requestBody { jsonBody<ProductOptionValueRequest>() }
        responses {
            created<ProductOptionValueResponse>("Option value created")
            standardErrors()
        }
    }

    /**
     * Seller - Delete an option value
     *
     */
    delete("{id}/options/{optionId}/values/{valueId}") {
        variantService.deleteOptionValue(
            call.currentUserId,
            call.requirePathParameter("id"),
            call.requirePathParameter("optionId"),
            call.requirePathParameter("valueId"),
        )
        call.respondOk(MessageResponse("Option value deleted successfully"))
    }.describe { summary = "Delete option value" }
}
