package com.piashcse.route

import com.piashcse.controller.BrandController
import com.piashcse.models.bands.AddBrand
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
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
                    queryParameter<Long>("offset") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("limit", "offset")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        brandController.getBrands(limit.toInt(), offset.toLong()), HttpStatusCode.OK
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
                val requiredParams = listOf("id", "name")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id, name) = requiredParams.map { call.parameters[it]!! }
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        brandController.deleteBrand(id), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}