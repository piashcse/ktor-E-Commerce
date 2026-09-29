package com.piashcse.feature.shop_category

import com.piashcse.model.request.ShopCategoryRequest
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public shop category routes.
 */
fun Route.shopCategoryRoutes() {
    val shopCategoryRepo: ShopCategoryRepository by inject()
    /**
     * @tag Shop-Category
     * @description Retrieve a paginated list of all shop categories
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopCategoryRepo.getCategories(limit, offset))
    }
}

/**
 * Admin shop category management routes.
 */
fun Route.shopCategoryAdminRoutes() {
    val shopCategoryRepo: ShopCategoryRepository by inject()
    adminWriteRateLimit {
        /**
         * @tag Shop-Category
         * @description Admin: Create a new shop category
         */
        post {
            call.respondCreated(shopCategoryRepo.createCategory(call.receive<ShopCategoryRequest>().name))
        }

        /**
         * @tag Shop-Category
         * @description Admin: Update an existing shop category name
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            val name = call.requireQueryParameter("name")
            call.respondOk(shopCategoryRepo.updateCategory(id, name))
        }

        /**
         * @tag Shop-Category
         * @description Admin: Permanently delete a shop category
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(shopCategoryRepo.deleteCategory(id))
        }
    }
}
