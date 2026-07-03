package com.piashcse.feature.shop_category

import com.piashcse.model.request.ShopCategoryRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Admin shop category management routes.
 */
fun Route.shopCategoryAdminRoutes(shopCategoryService: ShopCategoryService) {
    /**
     * @tag Shop-Category
     * @description Admin: Create a new shop category
     */
    post {
        val requestBody = call.receive<ShopCategoryRequest>()
        call.respond(
            HttpStatusCode.Created,
            shopCategoryService.createCategory(requestBody.name),
        )
    }

    /**
     * @tag Shop-Category
     * @description Admin: Update an existing shop category name
     */
    put("{id}") {
        val id = call.requirePathParameter("id")
        val name = call.requireQueryParameter("name")
        call.respond(
            HttpStatusCode.OK,
            shopCategoryService.updateCategory(id, name),
        )
    }

    /**
     * @tag Shop-Category
     * @description Admin: Permanently delete a shop category
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        call.respond(
            HttpStatusCode.OK,
            shopCategoryService.deleteCategory(id),
        )
    }
}
