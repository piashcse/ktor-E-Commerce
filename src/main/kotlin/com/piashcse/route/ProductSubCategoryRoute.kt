package com.piashcse.route

import com.piashcse.controller.ProductSubCategoryController
import com.piashcse.models.subcategory.AddProductSubCategory
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productSubCategoryRoute(subCategoryController: ProductSubCategoryController) {
    authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
        get("product-subcategory", {
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
            val (categoryId, limit, offset) = call.requiredParameters("categoryId", "limit", "offset") ?: return@get
            call.respond(
                ApiResponse.success(
                    subCategoryController.getProductSubCategory(categoryId, limit.toInt(), offset.toLong()),
                    HttpStatusCode.OK
                )
            )
        }
    }
    authenticate(RoleManagement.ADMIN.role) {
        post("product-subcategory", {
            tags("Product SubCategory")
            request {
                body<AddProductSubCategory>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<AddProductSubCategory>()
            call.respond(
                ApiResponse.success(
                    subCategoryController.addProductSubCategory(requestBody), HttpStatusCode.OK
                )
            )
        }
        put("product-subcategory/{id}", {
            tags("Product SubCategory")
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
                    subCategoryController.updateProductSubCategory(
                        id, name
                    ), HttpStatusCode.OK
                )
            )

        }
        delete("product-subcategory/{id}", {
            tags("Product SubCategory")
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
                    subCategoryController.deleteProductSubCategory(
                        id
                    ), HttpStatusCode.OK
                )
            )

        }
    }
}