package com.piashcse.feature.product_category

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public product category routes.
 */
fun Route.productCategoryRoutes() {
    val productCategoryRepo: ProductCategoryRepository by inject()
    /**
     * @tag Product-Category
     * @description Retrieve a paginated list of all product categories
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(productCategoryRepo.getCategories(limit, offset))
    }
}

/**
 * Admin product category management routes.
 */
fun Route.productCategoryAdminRoutes() {
    val productCategoryRepo: ProductCategoryRepository by inject()
    adminWriteRateLimit {
        /**
         * @tag Product-Category
         * @description Admin: Create a new product category
         */
        post {
            val name = call.requireQueryParameter("name")
            call.respondCreated(productCategoryRepo.createCategory(name))
        }

        /**
         * @tag Product-Category
         * @description Admin: Update an existing product category name
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            val name = call.requireQueryParameter("name")
            call.respondOk(productCategoryRepo.updateCategory(id, name))
        }

        /**
         * @tag Product-Category
         * @description Admin: Upload a category image
         */
        post("{id}/image") {
            val id = call.requirePathParameter("id")
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val bytes = UploadService.readAndValidateImagePart(part, "category image")
                    val fileName = UploadService.uploadCategoryImage(part, bytes)
                    imageUrl = UploadService.getCategoryImageUrl(fileName)
                }
                part.dispose()
            }
            val url = imageUrl ?: throw ValidationException(com.piashcse.constants.Message.Validation.FILE_REQUIRED)
            call.respondOk(productCategoryRepo.updateCategoryImage(id, url))
        }

        /**
         * @tag Product-Category
         * @description Admin: Permanently delete a product category
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            productCategoryRepo.deleteCategory(id)
            call.respondOk(com.piashcse.utils.common.MessageResponse("Category deleted successfully"))
        }
    }
}
