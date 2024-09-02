package com.piashcse.route

import com.piashcse.controller.ProductSubCategoryController
import com.piashcse.models.subcategory.AddProductSubCategory
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

fun Route.productSubCategoryRoute(subCategoryController: ProductSubCategoryController) {
    route("product-sub-category") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get({
                tags("Product SubCategory")
                request {
                    queryParameter<String>("categoryId") {
                        required = true
                    }
                    queryParameter<String>("limit") {
                        required = true
                    }
                    queryParameter<String>("offset") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("categoryId", "limit", "offset")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (categoryId, limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        subCategoryController.getProductSubCategory(categoryId, limit.toInt(), offset.toLong()),
                        HttpStatusCode.OK
                    )
                )
            }
        }
        authenticate(RoleManagement.ADMIN.role) {
            post({
                tags("Product SubCategory")
                request {
                    body<AddProductSubCategory>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddProductSubCategory>()
                requestBody.validation()
                call.respond(
                    ApiResponse.success(
                        subCategoryController.createProductSubCategory(requestBody), HttpStatusCode.OK
                    )
                )
            }
            put({
                tags("Product SubCategory")
                request {
                    queryParameter<String>("subCategoryId") {
                        required = true
                    }
                    queryParameter<String>("subCategoryName") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("subCategoryId", "subCategoryName")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (subCategoryId, subCategoryName) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        subCategoryController.updateProductSubCategory(
                            subCategoryId, subCategoryName
                        ), HttpStatusCode.OK
                    )
                )

            }
            delete({
                tags("Product SubCategory")
                request {
                    queryParameter<String>("subCategoryId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("subCategoryId")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (subCategoryId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        subCategoryController.deleteProductSubCategory(
                            subCategoryId
                        ), HttpStatusCode.OK
                    )
                )

            }
        }
    }
}