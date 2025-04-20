package com.piashcse.modules.brand.routes

import com.piashcse.modules.brand.controller.BrandController
import com.piashcse.database.models.bands.BrandRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.brandRoutes(brandController: BrandController) {
    route("brand") {
        /**
         * GET request to fetch a list of brands, with an optional limit on the number of brands.
         *
         * Accessible by customers, sellers, and admins.
         *
         * @param limit The maximum number of brands to return.
         */
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get({
                tags("Brand")
                summary = "auth[admin, customer, seller]"
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
         * Accessible only by admins.
         *
         * @param brandName The name of the brand to be created.
         */
        authenticate(RoleManagement.ADMIN.role) {
            post({
                tags("Brand")
                summary = "auth[admin]"
                request {
                    body<BrandRequest>()
                }
                apiResponse()
            }) {
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
             * Accessible only by admins.
             *
             * @param id The ID of the brand to be updated.
             * @param name The new name for the brand.
             */
            put("{id}", {
                tags("Brand")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("name") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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
             * Accessible only by admins.
             *
             * @param id The ID of the brand to be deleted.
             */
            delete("{id}", {
                tags("Brand")
                summary = "auth[admin]"
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
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