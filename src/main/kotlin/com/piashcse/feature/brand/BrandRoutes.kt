@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.brand

import com.piashcse.constants.Message
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.nameFromBodyOrQuery
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * Public brand routes.
 */
fun Route.brandRoutes() {
    val brandRepo: BrandRepository by inject()
    /**
     * Retrieve a paginated list of all brands (public)
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(brandRepo.getBrands(limit, offset))
    }.describe { summary = "Retrieve a paginated list of all brands (public)" }
}

/**
 * Admin brand management routes.
 */
fun Route.brandAdminRoutes() {
    val brandRepo: BrandRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin - Create a new brand
         *
         */
        post {
            call.respondCreated(brandRepo.createBrand(call.nameFromBodyOrQuery()))
        }.describe { summary = "Admin - Create a new brand" }

        /**
         * Admin - Update an existing brand
         *
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(brandRepo.updateBrand(id, call.nameFromBodyOrQuery()))
        }.describe { summary = "Admin - Update an existing brand" }

        /**
         * Admin - Upload a brand logo
         *
         */
        post("{id}/image") {
            val id = call.requirePathParameter("id")
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val bytes = UploadService.readAndValidateImagePart(part, "brand image")
                    val fileName = UploadService.uploadBrandImage(part, bytes)
                    imageUrl = UploadService.getBrandImageUrl(fileName)
                }
                part.dispose()
            }
            val url = imageUrl ?: throw ValidationException(Message.Validation.FILE_REQUIRED)
            call.respondOk(brandRepo.updateBrandLogo(id, url))
        }.describe { summary = "Admin - Upload a brand logo" }

        /**
         * Admin - Delete a brand
         *
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            brandRepo.deleteBrand(id)
            call.respondOk(com.piashcse.utils.common.MessageResponse("Brand deleted successfully"))
        }.describe { summary = "Admin - Delete a brand" }
    }
}
