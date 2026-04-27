package com.piashcse.feature.product_category

import com.piashcse.model.request.ProductCategoryRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public product category routes.
 */
fun Route.productCategoryRoutes(productCategoryService: ProductCategoryService) {
    /**
     * @tag ProductCategory
     * @description Retrieve a paginated list of all product categories
     */
    get {
        val (limit, offset) = call.paginationParameters()
        call.respond(
            HttpStatusCode.OK,
            productCategoryService.getCategories(limit, offset)
        )
    }
}

/**
 * Admin product category management routes.
 */
fun Route.productCategoryAdminRoutes(productCategoryService: ProductCategoryService) {
    adminAuth {
        /**
         * @tag ProductCategory
         * @description Admin: Create a new product category
         */
        post {
            val requestBody = call.receive<ProductCategoryRequest>()
            call.respond(
                HttpStatusCode.OK,
                productCategoryService.createCategory(requestBody.name)
            )
        }

        /**
         * @tag ProductCategory
         * @description Admin: Update an existing product category name
         */
        put("{id}") {
            val params = call.requireParameters("id", "name")
            call.respond(
                HttpStatusCode.OK,
                productCategoryService.updateCategory(params[0], params[1])
            )
        }

        /**
         * @tag ProductCategory
         * @description Admin: Permanently delete a product category
         */
        delete("{id}") {
            val id = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                productCategoryService.deleteCategory(id.first())
            )
        }
    }
}
