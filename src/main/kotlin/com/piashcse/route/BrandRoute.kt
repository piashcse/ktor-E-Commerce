package com.piashcse.route

import com.piashcse.controller.BrandController
import com.piashcse.models.bands.AddBrand
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.brandRoute(brandController: BrandController) {
    route("brand") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get("", {
                tags("Brand")
                request {
                    queryParameter<Int>("limit"){
                        required = true
                    }
                    queryParameter<Long>("offset"){
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("limit", "offset")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        brandController.getBrand(limit.toInt(), offset.toLong()), HttpStatusCode.OK
                    )
                )
            }
        }
        authenticate(RoleManagement.ADMIN.role) {
            post("", {
                tags("Brand")
                request {
                    body<AddBrand>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddBrand>()
                requestBody.validation()
                call.respond(
                    ApiResponse.success(
                        brandController.createBrand(requestBody.brandName), HttpStatusCode.OK
                    )
                )
            }
            put("", {
                tags("Brand")
                request {
                    queryParameter<String>("id"){
                        required = true
                    }
                    queryParameter<String>("brandName"){
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("id", "brandName")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id, brandName) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        brandController.updateBrand(id, brandName), HttpStatusCode.OK
                    )
                )
            }
            delete("", {
                tags("Brand")
                request {
                    queryParameter<String>("id"){
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
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