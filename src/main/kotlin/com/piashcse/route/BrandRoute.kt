package com.piashcse.route

import com.piashcse.controller.BrandController
import com.piashcse.models.bands.AddBrand
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

fun Route.brandRoute(brandController: BrandController) {
    route("brand") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get({
                tags("Brand")
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
        authenticate(RoleManagement.ADMIN.role) {
            post({
                tags("Brand")
                request {
                    body<AddBrand>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddBrand>()
                call.respond(
                    ApiResponse.success(
                        brandController.addBrand(requestBody.brandName), HttpStatusCode.OK
                    )
                )
            }
            put("{id}", {
                tags("Brand")
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
            delete("{id}", {
                tags("Brand")
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