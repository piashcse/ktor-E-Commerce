package com.piashcse.route

import com.piashcse.controller.ProductCategoryController
import com.piashcse.models.category.AddProductCategory
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
    authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
        get("category", {
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
        post("category", {
            tags("Product Category")
            request {
                body<AddProductCategory>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<AddProductCategory>()
            call.respond(
                ApiResponse.success(
                    productCategoryController.addProductCategory(
                        requestBody.categoryName
                    ), HttpStatusCode.OK
                )
            )
        }
        put("category/{id}", {
            tags("Product Category")
            request {
                pathParameter<String>("id") {
                    required = true
                }
                queryParameter<String>("categoryName") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val categoryId = call.parameters["id"]!!
            val categoryName = call.parameters["categoryName"]!!
            call.respond(
                ApiResponse.success(
                    productCategoryController.updateProductCategory(
                        categoryId, categoryName
                    ), HttpStatusCode.OK
                )
            )
        }
        delete("category/{id}", {
            tags("Product Category")
            request {
                pathParameter<String>("id") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val categoryId = call.parameters["id"]!!
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