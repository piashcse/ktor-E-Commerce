package com.piashcse.feature.shop_category

import com.piashcse.model.request.ShopCategoryRequest
import com.piashcse.plugin.adminAuth
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Admin shop category management routes.
 */
fun Route.shopCategoryAdminRoutes(shopCategoryController: ShopCategoryService) {
    adminAuth {
        /**
         * @tag ShopCategory
         * @description Admin: Create a new shop category
         */
        post {
            val requestBody = call.receive<ShopCategoryRequest>()
            call.respond(
                HttpStatusCode.OK,
                shopCategoryController.createCategory(requestBody.name)
            )
        }

        /**
         * @tag ShopCategory
         * @description Admin: Retrieve a paginated list of all shop categories
         */
        get {
            val (limit, offset) = call.paginationParameters()
            call.respond(
                HttpStatusCode.OK,
                shopCategoryController.getCategories(limit, offset)
            )
        }

        /**
         * @tag ShopCategory
         * @description Admin: Permanently delete a shop category
         */
        delete("{id}") {
            val id = call.requireParameters("id")
            call.respond(
                HttpStatusCode.OK,
                shopCategoryController.deleteCategory(id.first())
            )
        }

        /**
         * @tag ShopCategory
         * @description Admin: Update an existing shop category name
         */
        put("{id}") {
            val params = call.requireParameters("id", "name")
            call.respond(
                HttpStatusCode.OK,
                shopCategoryController.updateCategory(params[0], params[1])
            )
        }
    }
}
