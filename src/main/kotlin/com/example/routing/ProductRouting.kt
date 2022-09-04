package com.example.routing

import com.example.controller.ProductController
import com.example.models.product.AddCategoryBody
import com.example.models.product.AddProduct
import com.example.models.product.VariantOptionName
import com.example.models.user.JwtTokenBody
import com.example.utils.AppConstants
import com.example.utils.CustomResponse
import com.example.utils.Response
import com.example.utils.extension.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.productRoute(productController: ProductController) {
    route("product/") {
        authenticateWithJwt(AppConstants.RoleManagement.ADMIN) {
            route("category").post<Unit, Response, AddCategoryBody, JwtTokenBody> { response, addCategory ->
                addCategory.validation()
                respond(CustomResponse.success(productController.createProductCategory(addCategory), HttpStatusCode.OK))
            }


            route("color").post<Unit, Response, VariantOptionName, JwtTokenBody> { response, addColor ->
                addColor.validation()
                respond(CustomResponse.success(productController.createDefaultColorOption(addColor.variantOptionName), HttpStatusCode.OK))
            }
        }
        authenticateWithJwt(AppConstants.RoleManagement.ADMIN, AppConstants.RoleManagement.MERCHANT) {
            post<Unit, Response, AddProduct, JwtTokenBody> { response, addProduct ->
                addProduct.validation()
                respond(CustomResponse.success(productController.createProduct(addProduct), HttpStatusCode.OK))
            }
            /*route("upload-product-image").post<Unit, Response, VariantOptionName, JwtTokenBody> { response, addColor ->
                addColor.validation()
                val db = productController.createDefaultColorOption(addColor.variantOptionName)
                db.let {
                    respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }*/

        }
    }
    /*route("product/") {
        authenticate(AppConstants.RoleManagement.ADMIN) {
            post("add-category") {
                val addCategory = call.receive<AddCategoryBody>()
                addCategory.validation()
                val db = productController.createProductCategory(addCategory)
                db.let {
                    call.respond(CustomResponse.success(db, HttpStatusCode.OK))
                }
            }

            post("add-color") {
                val addColor = call.receive<VariantOptionName>()
                addColor.validation()
                val db = productController.createDefaultColorOption(addColor.variantOptionName)
                db.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }
            post("add-size") {
                val addSize = call.receive<VariantOptionName>()
                addSize.validation()
                val db = productController.createDefaultSizeOption(addSize.variantOptionName)

                db.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }

        }
        authenticate(AppConstants.RoleManagement.ADMIN, AppConstants.RoleManagement.MERCHANT) {
            post("upload-product-image") {
                val multipart = call.receiveMultipart()
                val images = arrayListOf<String>()
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            println("${part.name} : ${part.value}")
                        }
                        is PartData.FileItem -> {
                            //File("${AppConstants.Image.PROFILE_IMAGE_LOCATION}Screenshot 2021-12-22 at 10.37.21 PM.png").delete()
                            val fileName = part.originalFileName as String
                            val fileBytes = part.streamProvider().readBytes()
                            val fileNameInServer =
                                "${AppConstants.Image.PRODUCT_IMAGE_LOCATION}${UUID.randomUUID()}.${fileName.fileExtension()}"
                            File(fileNameInServer).writeBytes(fileBytes)
                            images += fileNameInServer
                        }
                        else -> {
                            call.respond(CustomResponse.failure(ErrorMessage.IMAGE_UPLOAD_FAILED, HttpStatusCode.OK))
                        }
                    }
                    part.dispose
                }
                val db = productController.uploadProductImages(images.joinToString("."))
                db.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                }
            }
        }
        post("add-product") {
            val addProduct = call.receive<AddProduct>()
            addProduct.validation()
            val db = productController.createProduct(addProduct)
            db.let {
                call.respond(CustomResponse.success(it, HttpStatusCode.OK))
            }
        }
    }*/
}