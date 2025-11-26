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
        /**
         * GET request to fetch a list of brands, with an optional limit on the number of brands.
         *
         * @tag Brand
         * @summary auth[admin, customer, seller]
         * @query limit The maximum number of brands to return.
         * @response 200 [ApiResponse] Success response with brands
         * @response 400 Bad request if limit is missing
         */
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get("/brand") {
                val (limit) = call.requiredParameters("limit") ?: return@get
                call.respond(
                    ApiResponse.success(
                        brandController.getBrands(limit.toInt()), HttpStatusCode.OK
                    )
                )
            }
        }

        /**
         * POST request to create a new brand.
         *
         * @tag Brand
         * @summary auth[admin]
         * @body [BrandRequest] The name of the brand to be created.
         * @response 200 [ApiResponse] Success response after creation
         */
        authenticate(RoleManagement.ADMIN.role) {
            post("/brand") {
                val requestBody = call.receive<BrandRequest>()
                call.respond(
                    ApiResponse.success(
                        brandController.createBrand(requestBody.name), HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to update an existing brand's name.
             *
             * @tag Brand
             * @summary auth[admin]
             * @path id The ID of the brand to be updated.
             * @query name The new name for the brand.
             * @response 200 [ApiResponse] Success response after update
             * @response 400 Bad request if required parameters are missing
             */
            put("/brand/{id}") {
                val (id, name) = call.requiredParameters("id", "name") ?: return@put
                call.respond(
                    ApiResponse.success(
                        brandController.updateBrand(id, name), HttpStatusCode.OK
                    )
                )
            }

            /**
             * DELETE request to remove a brand by its ID.
             *
             * @tag Brand
             * @summary auth[admin]
             * @path id The ID of the brand to be deleted.
             * @response 200 [ApiResponse] Success response after deletion
             * @response 400 Bad request if id is missing
             */
            delete("/brand/{id}") {
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