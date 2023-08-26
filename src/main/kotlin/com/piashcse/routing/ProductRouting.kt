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
import com.piashcse.models.user.body.MultipartImage
import com.piashcse.models.user.body.UserId
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.fileExtension
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

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
            post<Unit, Response, AddProduct, JwtTokenBody> { _, requestBody ->
                requestBody.validation()
                respond(ApiResponse.success(productController.addProduct(requestBody), HttpStatusCode.OK))
            }
            delete<ProductId, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(productController.deleteProduct(params), HttpStatusCode.OK))
            }
            route("product-photo-upload").post<UserId, Response, MultipartImage, JwtTokenBody> { params, multipartData ->
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
            }
        }
    }
}