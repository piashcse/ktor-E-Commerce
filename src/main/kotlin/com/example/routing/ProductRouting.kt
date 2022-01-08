package com.example.routing

import com.example.controller.ProductController
import com.example.models.product.AddCategoryBody
import com.example.models.product.AddProduct
import com.example.models.product.VariantOptionName
import com.example.utils.AppConstants
import com.example.utils.CustomResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoute(categoryController: ProductController) {
    route("product/") {
        authenticate(AppConstants.RoleManagement.ADMIN) {
            post("add-category") {
                val addCategory = call.receive<AddCategoryBody>()
                addCategory.validation()
                val db = categoryController.createProductCategory(addCategory.categoryName)
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }

            post("add-color") {
                val addColor = call.receive<VariantOptionName>()
                addColor.validation()
                val db = categoryController.createDefaultColorOption(addColor.variantOptionName)
                db.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }
            post("add-size") {
                val addSize = call.receive<VariantOptionName>()
                addSize.validation()
                val db = categoryController.createDefaultSizeOption(addSize.variantOptionName)

                db.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }

        }
        authenticate(AppConstants.RoleManagement.ADMIN, AppConstants.RoleManagement.MERCHANT) {
            post("add-product") {
                val addProduct = call.receive<AddProduct>()
                addProduct.validation()
                val db = categoryController.createProduct(addProduct)
                db.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
    }
}