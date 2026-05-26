package com.piashcse.feature.brand

import com.piashcse.constants.UserType
import com.piashcse.model.request.BrandRequest
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.paginateQueryParams
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public brand routes.
 */
fun Route.brandRoutes(brandService: BrandService) {
    requireRole(UserType.CUSTOMER, UserType.SELLER, UserType.ADMIN) {
        /**
         * @tag Brand
         * @description Retrieve a paginated list of all brands
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respond(
                HttpStatusCode.OK,
                brandService.getBrands(limit, offset),
            )
        }
    }
}

/**
 * Admin brand management routes.
 */
fun Route.brandAdminRoutes(brandService: BrandService) {
    /**
     * @tag Brand
     * @description Admin: Create a new brand
     */
    post {
        val requestBody = call.receive<BrandRequest>()
        call.respond(
            HttpStatusCode.OK,
            brandService.createBrand(requestBody.name),
        )
    }

    /**
     * @tag Brand
     * @description Admin: Update an existing brand
     */
    put("{id}") {
        val id = call.requirePathParameter("id")
        val name = call.requireQueryParameter("name")
        call.respond(
            HttpStatusCode.OK,
            brandService.updateBrand(id, name),
        )
    }

    /**
     * @tag Brand
     * @description Admin: Delete a brand
     */
    delete("{id}") {
        val id = call.requirePathParameter("id")
        call.respond(
            HttpStatusCode.OK,
            brandService.deleteBrand(id),
        )
    }
}
