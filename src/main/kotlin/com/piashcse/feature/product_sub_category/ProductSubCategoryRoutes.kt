package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public product subcategory routes.
 */
fun Route.productSubCategoryRoutes(subCategoryController: ProductSubCategoryService) {
    /**
     * @tag ProductSubCategory
     * @description Retrieve subcategories for a specific category
     */
    get {
        val categoryId = call.parameters["categoryId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "categoryId is required")
        val (limit, offset) = call.paginationParameters()
        call.respond(
            HttpStatusCode.OK,
            subCategoryController.getProductSubCategory(categoryId, limit, offset)
        )
    }
}

/**
 * Admin product subcategory management routes.
 */
fun Route.productSubCategoryAdminRoutes(subCategoryController: ProductSubCategoryService) {
    adminAuth {
        /**
         * @tag ProductSubCategory
         * @description Admin: Create a new product subcategory
         */
        post {
            val requestBody = call.receive<ProductSubCategoryRequest>()
            call.respond(
                HttpStatusCode.OK,
                subCategoryController.addProductSubCategory(requestBody)
            )
        }

        /**
         * @tag ProductSubCategory
         * @description Admin: Update an existing product subcategory name
         */
        put("{id}") {
            val params = call.requireParameters("id", "name")
            call.respond(
                HttpStatusCode.OK,
                subCategoryController.updateProductSubCategory(params[0], params[1])
            )
        }

        /**
         * @tag ProductSubCategory
         * @description Admin: Permanently delete a product subcategory
         */
        delete("{id}") {
            val id = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                subCategoryController.deleteProductSubCategory(id.first())
            )
        }
    }
}
