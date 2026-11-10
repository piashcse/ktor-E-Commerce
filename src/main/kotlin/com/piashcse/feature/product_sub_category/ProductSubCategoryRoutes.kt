@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.utils.extension.nameFromBodyOrQuery
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * Public product subcategory routes.
 */
fun Route.productSubCategoryRoutes() {
    val subCategoryRepo: ProductSubCategoryRepository by inject()
    /**
     * Retrieve subcategories for a specific category
     */
    get {
        val categoryId = call.requireQueryParameter("categoryId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(subCategoryRepo.getProductSubCategory(categoryId, limit, offset))
    }.describe { summary = "Retrieve subcategories for a specific category" }
}

/**
 * Admin product subcategory management routes.
 */
fun Route.productSubCategoryAdminRoutes() {
    val subCategoryRepo: ProductSubCategoryRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Create a new product subcategory
         *
         */
        post {
            call.respondCreated(subCategoryRepo.addProductSubCategory(call.receive<ProductSubCategoryRequest>()))
        }.describe { summary = "Admin - Create a new product subcategory" }

        /**
         * Admin - Update an existing product subcategory name
         *
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(subCategoryRepo.updateProductSubCategory(id, call.nameFromBodyOrQuery()))
        }.describe { summary = "Admin - Update an existing product subcategory name" }

        /**
         * Admin - Permanently delete a product subcategory
         *
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            subCategoryRepo.deleteProductSubCategory(id)
            call.respondDeleted("Subcategory deleted successfully")
        }.describe { summary = "Admin - Permanently delete a product subcategory" }
    }
}
