package com.piashcse.route

import com.piashcse.controller.ProductCategoryController
import com.piashcse.models.category.AddProductCategory
import com.piashcse.models.product.request.AddProduct
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productCategoryRoute(productCategoryController: ProductCategoryController) {
    route("product-category") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get("", {
                tags("Product Category")
                request {
                    queryParameter<String>("limit") {
                        required = true
                    }
                    queryParameter<String>("offset") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("limit", "offset")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(
                        ApiResponse.success(
                            "Missing parameters: $it", HttpStatusCode.OK
                        )
                    )
                }
                val (limit, offset) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        productCategoryController.getProductCategory(
                            limit.toInt(), offset.toLong()
                        ), HttpStatusCode.OK
                    )
                )
            }
        }

        authenticate(RoleManagement.ADMIN.role) {
            post("", {
                tags("Product Category")
                request {
                    body<AddProductCategory>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddProductCategory>()
                requestBody.validation()
                call.respond(
                    ApiResponse.success(
                        productCategoryController.createProductCategory(
                            requestBody.categoryName
                        ), HttpStatusCode.OK
                    )
                )
            }
            put("", {
                tags("Product Category")
                request {
                    queryParameter<String>("categoryId") {
                        required = true
                    }
                    queryParameter<String>("categoryName") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("categoryId", "categoryName")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(
                        ApiResponse.success(
                            "Missing parameters: $it", HttpStatusCode.OK
                        )
                    )
                }
                val (categoryId, categoryName) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        productCategoryController.updateProductCategory(
                            categoryId, categoryName
                        ), HttpStatusCode.OK
                    )
                )
            }
            delete("", {
                tags("Product Category")
                request {
                    queryParameter<String>("categoryId") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("categoryId")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(
                        ApiResponse.success(
                            "Missing parameters: $it", HttpStatusCode.OK
                        )
                    )
                }
                val (categoryId) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        productCategoryController.deleteProductCategory(
                            categoryId
                        ), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}/*
fun NormalOpenAPIRoute.productCategoryRoute(productCategoryController: ProductCategoryController) {
    route("product-category") {
        authenticateWithJwt(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get<PagingData, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(productCategoryController.getProductCategory(params), HttpStatusCode.OK))
            }
        }
        authenticateWithJwt(RoleManagement.ADMIN.role) {
            post<AddProductCategory, Response, Unit, JwtTokenBody>{ params, _ ->
                params.validation()
                respond(ApiResponse.success(productCategoryController.createProductCategory(params), HttpStatusCode.OK))
            }
            put<UpdateProductCategory, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(ApiResponse.success(productCategoryController.updateProductCategory(params), HttpStatusCode.OK))
            }
            delete<DeleteProductCategory, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        productCategoryController.deleteProductCategory(params), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}*/
