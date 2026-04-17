package com.piashcse.feature.brand

import com.piashcse.model.request.BrandRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.extension.paginationParameters
import com.piashcse.utils.extension.requireParameters
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
    authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            /**
             * @tag Brand
             * @description Retrieve a paginated list of all brands
             * @operationId getBrands
             * @query limit Maximum number of brands to return (default 20)
             * @query offset Number of brands to skip (default 0)
             * @response 200 List of brands retrieved successfully
             * @response 400 Invalid limit parameter
             * @security jwtToken
             */
            get {
                val (limit, offset) = call.paginationParameters()
                call.respond(
                    HttpStatusCode.OK,
                    brandController.getBrands(limit, offset)
                )
            }
        }

        authenticate(RoleManagement.ADMIN.role) {
            /**
             * @tag Brand
             * @description Create a new brand with the provided name
             * @operationId createBrand
             * @body BrandRequest Brand creation request with name
             * @response 200 Brand created successfully
             * @security jwtToken
             */
            post {
                val requestBody = call.receive<BrandRequest>()
                call.respond(
                    HttpStatusCode.OK,
                    brandController.createBrand(requestBody.name)
                )
            }

            /**
             * @tag Brand
             * @description Update the name of an existing brand by ID
             * @operationId updateBrand
             * @path id (required) Unique identifier of the brand to update
             * @query name (required) New name for the brand
             * @response 200 Brand updated successfully
             * @response 400 Invalid brand ID or name parameter
             * @security jwtToken
             */
            put("{id}") {
                val params = call.requireParameters("id", "name")
                call.respond(
                    HttpStatusCode.OK,
                    brandController.updateBrand(params[0], params[1])
                )
            }

            /**
             * @tag Brand
             * @description Permanently delete a brand by its ID
             * @operationId deleteBrand
             * @path id (required) Unique identifier of the brand to delete
             * @response 200 Brand deleted successfully
             * @response 400 Invalid brand ID
             * @security jwtToken
             */
            delete("{id}") {
                val id = call.requireParameters("id")
                call.respond(
                    HttpStatusCode.OK,
                    brandController.deleteBrand(id.first())
                )
            }
        }
    }
