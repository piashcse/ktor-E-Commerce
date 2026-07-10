package com.piashcse.feature.product_sub_category

import com.piashcse.model.request.ProductSubCategoryRequest
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public product subcategory routes.
 */
fun Route.productSubCategoryRoutes() {
    val subCategoryRepo: ProductSubCategoryRepository by inject()
    /**
     * @tag Product-Sub-Category
     * @description Retrieve subcategories for a specific category
     */
    get {
        val categoryId = call.requireQueryParameter("categoryId")
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(subCategoryRepo.getProductSubCategory(categoryId, limit, offset))
    }
}

/**
 * Admin product subcategory management routes.
 */
fun Route.productSubCategoryAdminRoutes() {
    val subCategoryRepo: ProductSubCategoryRepository by inject()
    /**
     * @tag Product-Sub-Category
     * @description Admin: Create a new product subcategory
     */
    post {
        call.respondCreated(subCategoryRepo.addProductSubCategory(call.receive<ProductSubCategoryRequest>()))
    }

    /**
     * @tag Product-Sub-Category
     * @description Admin: Update an existing product subcategory name
     */
    put("{id}") {
        val id = call.requirePathParameter("id")
        val name = call.requireQueryParameter("name")
        call.respondOk(subCategoryRepo.updateProductSubCategory(id, name))
    }

    /**
     * @tag Product-Sub-Category
     * @description Admin: Permanently delete a product subcategory
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(subCategoryRepo.deleteProductSubCategory(id))
    }
}
