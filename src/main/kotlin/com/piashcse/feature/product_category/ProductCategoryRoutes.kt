package com.piashcse.feature.product_category

import com.piashcse.model.request.ProductCategoryRequest
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
     * @tag Product Category
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
    /**
     * @tag Product Category
     * @description Admin: Create a new product category
     */
    post {
        val (name) = call.requireParameters("name")
        call.respond(HttpStatusCode.OK, productCategoryService.createCategory(name))
    }

    /**
     * @tag Product Category
     * @description Admin: Update an existing product category name
     */
    put("{id}") {
        val (id, name) = call.requireParameters("id", "name")
        call.respond(HttpStatusCode.OK, productCategoryService.updateCategory(id, name))
    }

    /**
     * @tag Product Category
     * @description Admin: Permanently delete a product category
     */
    delete("{id}") {
        val (id) = call.requireParameters("id")
        call.respond(HttpStatusCode.OK, productCategoryService.deleteCategory(id))
    }
}
