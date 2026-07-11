package com.piashcse.feature.brand

import com.piashcse.constants.UserType
import com.piashcse.model.request.BrandRequest
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Public brand routes.
 */
fun Route.brandRoutes() {
    val brandRepo: BrandRepository by inject()
    requireRole(UserType.CUSTOMER, UserType.SELLER, UserType.ADMIN) {
        /**
         * @tag Brand
         * @description Retrieve a paginated list of all brands
         */
        get {
            val (limit, offset) = call.paginateQueryParams()
            call.respondOk(brandRepo.getBrands(limit, offset))
        }
    }
}

/**
 * Admin brand management routes.
 */
fun Route.brandAdminRoutes() {
    val brandRepo: BrandRepository by inject()
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) {
        /**
         * @tag Brand
         * @description Admin: Create a new brand
         */
        post {
            call.respondCreated(brandRepo.createBrand(call.receive<BrandRequest>().name))
        }

        /**
         * @tag Brand
         * @description Admin: Update an existing brand
         */
        put("{id}") {
            val id = call.requirePathParameter("id")
            val name = call.requireQueryParameter("name")
            call.respondOk(brandRepo.updateBrand(id, name))
        }

        /**
         * @tag Brand
         * @description Admin: Delete a brand
         */
        delete("{id}") {
            val id = call.requirePathParameter("id")
            call.respondOk(brandRepo.deleteBrand(id))
        }
    }
}
