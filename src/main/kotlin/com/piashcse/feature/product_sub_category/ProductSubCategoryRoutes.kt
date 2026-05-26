package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.utils.extension.paginationParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public product subcategory routes.
 */
fun Route.productSubCategoryRoutes(subCategoryService: ProductSubCategoryService) {
    /**
     * @tag Product-Sub-Category
     * @description Retrieve subcategories for a specific category
     */
    get {
        val categoryId = call.requireQueryParameter("categoryId")
        val (limit, offset) = call.paginationParameters()
        call.respond(
            HttpStatusCode.OK,
            subCategoryService.getProductSubCategory(categoryId, limit, offset),
        )
    }
}

/**
 * Admin product subcategory management routes.
 */
fun Route.productSubCategoryAdminRoutes(subCategoryService: ProductSubCategoryService) {
    /**
     * @tag Product-Sub-Category
     * @description Admin: Create a new product subcategory
     */
    post {
        val requestBody = call.receive<ProductSubCategoryRequest>()
        call.respond(
            HttpStatusCode.OK,
            subCategoryService.addProductSubCategory(requestBody),
        )
    }

    /**
     * @tag Product-Sub-Category
     * @description Admin: Update an existing product subcategory name
     */
    put("{id}") {
        val id = call.requirePathParameter("id")
        val name = call.requireQueryParameter("name")
        call.respond(
            HttpStatusCode.OK,
            subCategoryService.updateProductSubCategory(id, name),
        )
    }

    /**
     * @tag Product-Sub-Category
     * @description Admin: Permanently delete a product subcategory
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        call.respond(
            HttpStatusCode.OK,
            subCategoryService.deleteProductSubCategory(id),
        )
    }
}
