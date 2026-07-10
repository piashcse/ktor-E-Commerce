package com.piashcse.feature.shop_category

import com.piashcse.model.request.ShopCategoryRequest
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Admin shop category management routes.
 */
fun Route.shopCategoryAdminRoutes() {
    val shopCategoryRepo: ShopCategoryRepository by inject()
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
