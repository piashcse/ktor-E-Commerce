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
             * @summary auth[admin, customer, seller]
             * @query limit The maximum number of brands to return. (required)
             * @response 200 [ApiResponse] Success response with brands
             * @response 400 Bad request if limit is missing
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
             * @summary auth[admin]
             * @body [BrandRequest] The name of the brand to be created.
             * @response 200 [ApiResponse] Success response after creation
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
             * @summary auth[admin]
             * @path id The ID of the brand to be updated.
             * @query name The new name for the brand.
             * @response 200 [ApiResponse] Success response after update
             * @response 400 Bad request if required parameters are missing
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
             * @summary auth[admin]
             * @path id The ID of the brand to be deleted.
             * @response 200 [ApiResponse] Success response after deletion
             * @response 400 Bad request if id is missing
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