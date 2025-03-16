package com.piashcse.route

import com.piashcse.controller.ProductCategoryController
import com.piashcse.models.category.AddProductCategory
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

fun Route.productCategoryRoute(productCategoryController: ProductCategoryController) {
    authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
        get("product-category", {
            tags("Product Category")
            request {
                queryParameter<String>("limit") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (limit) = call.requiredParameters("limit") ?: return@get
            call.respond(
                ApiResponse.success(
                    productCategoryController.getProductCategory(
                        limit.toInt()
                    ), HttpStatusCode.OK
                )
            )
        }
    }

    authenticate(RoleManagement.ADMIN.role) {
        post("product-category", {
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
        put("product-category/{id}", {
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
            val (id, categoryName) = call.requiredParameters("id", "categoryName") ?: return@put
            call.respond(
                ApiResponse.success(
                    productCategoryController.updateProductCategory(
                        id, categoryName
                    ), HttpStatusCode.OK
                )
            )
        }
        delete("product-category/{id}", {
            tags("Product Category")
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
                    productCategoryController.deleteProductCategory(
                        id
                    ), HttpStatusCode.OK
                )
            )
        }
    }
}