@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.shop_category

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
 * Public shop category routes.
 */
fun Route.shopCategoryRoutes() {
    val shopCategoryRepo: ShopCategoryRepository by inject()
    /**
     * Retrieve a paginated list of all shop categories
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(shopCategoryRepo.getCategories(limit, offset))
    }.describe { summary = "Retrieve a paginated list of all shop categories" }
}

/**
 * Admin shop category management routes.
 */
fun Route.shopCategoryAdminRoutes() {
    val shopCategoryRepo: ShopCategoryRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Create a new shop category
         *
         */
        post {
            call.respondCreated(shopCategoryRepo.createCategory(call.nameFromBodyOrQuery()))
        }.describe { summary = "Admin - Create a new shop category" }

        /**
         * Admin - Update an existing shop category name
         *
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(shopCategoryRepo.updateCategory(id, call.nameFromBodyOrQuery()))
        }.describe { summary = "Admin - Update an existing shop category name" }

        /**
         * Admin - Permanently delete a shop category
         *
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            shopCategoryRepo.deleteCategory(id)
            call.respondDeleted("Shop category deleted successfully")
        }.describe { summary = "Admin - Permanently delete a shop category" }
    }
}
