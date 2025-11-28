package com.piashcse.feature.brand

import com.piashcse.model.request.BrandRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for managing brands, with authentication and authorization based on user roles.
 *
 * @param brandController The controller handling brand-related operations.
 */
fun Route.brandRoutes(brandController: BrandService) {
    route("brand") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            /**
             * @tag Brand
             * @query limit (required)
             * @response 200 [Response]
             * @response 400
             */
            get {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        brandController.getBrands(limit.toInt()), HttpStatusCode.OK
                    )
                )
            }
        }

        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag Brand
             * @body [BrandRequest]
             * @response 200 [Response]
             */
            post {
                val requestBody = call.receive<BrandRequest>()
                call.respond(
                    ApiResponse.success(
                        brandController.createBrand(requestBody.name), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Brand
             * @path id (required)
             * @query name (required)
             * @response 200 [Response]
             * @response 400
             */
            put("{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                call.respond(
                    ApiResponse.success(
                        brandController.updateBrand(id, name), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Brand
             * @path id (required)
             * @response 200 [ApiResponse]
             * @response 400
             */
            delete("{id}") {
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        brandController.deleteBrand(id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}