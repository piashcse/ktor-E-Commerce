package com.piashcse.routing

import com.piashcse.controller.ProductController
import com.piashcse.models.product.request.*
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.delete
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.productRoute(productController: ProductController) {
    route("product") {
        authenticateWithJwt(RoleManagement.USER.role, RoleManagement.SELLER.role) {
            get<ProductWithFilter, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(productController.getProduct(params), HttpStatusCode.OK))
            }
            route("/{productId}").get<ProductDetail, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(productController.productDetail(params), HttpStatusCode.OK))
            }
        }
        authenticateWithJwt(RoleManagement.SELLER.role) {
            post<Unit, Response, AddProduct, JwtTokenBody> { _, productRequestBody ->
                productRequestBody.validation()
                respond(ApiResponse.success(productController.createProduct(productRequestBody), HttpStatusCode.OK))
            }
            delete<DeleteProduct, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(productController.deleteProduct(params), HttpStatusCode.OK))
            }
            /*route("image-upload").post<UserId, Response, MultipartImage, JwtTokenBody> { params, multipartData ->
                params.validation()
                multipartData.validation()

                UUID.randomUUID()?.let { imageId ->
                    val fileLocation = multipartData.file.name?.let {
                        "${AppConstants.Image.PRODUCT_IMAGE_LOCATION}$imageId${it.fileExtension()}"
                    }
                    fileLocation?.let {
                        File(it).writeBytes(withContext(Dispatchers.IO) {
                            multipartData.file.readAllBytes()
                        })
                    }
                    val fileNameInServer = imageId.toString().plus(fileLocation?.fileExtension())
                    respond(
                        ApiResponse.success(
                            productController.uploadProductImages(params.userId, fileNameInServer), HttpStatusCode.OK
                        )
                    )
                }
            }*/
        }
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