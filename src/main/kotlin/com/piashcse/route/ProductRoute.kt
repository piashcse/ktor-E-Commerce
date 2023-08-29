package com.piashcse.route

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
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.auth.put
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
        authenticateWithJwt(RoleManagement.USER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            route("/{productId}").get<ProductDetail, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(productController.productDetail(params), HttpStatusCode.OK))
            }
            get<ProductWithFilter, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        productController.getProductById(principal().userId, params), HttpStatusCode.OK
                    )
                )
            }
        }
        authenticateWithJwt(RoleManagement.SELLER.role) {
            get<ProductWithFilter, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        productController.getProductById(principal().userId, params), HttpStatusCode.OK
                    )
                )
            }
            post<Unit, Response, AddProduct, JwtTokenBody> { _, requestBody ->
                requestBody.validation()
                respond(
                    ApiResponse.success(
                        productController.addProduct(principal().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            route("/{productId}").put<ProductIdPathParam, Response, UpdateProduct, JwtTokenBody> { params, requestBody ->
                respond(
                    ApiResponse.success(
                        productController.updateProduct(principal().userId, params.productId, requestBody),
                        HttpStatusCode.OK
                    )
                )
            }
            delete<ProductId, Response, JwtTokenBody> { params ->
                params.validation()
                respond(
                    ApiResponse.success(
                        productController.deleteProduct(principal().userId, params), HttpStatusCode.OK
                    )
                )
            }
            route("photo-upload").post<ProductId, Response, MultipartImage, JwtTokenBody> { params, multipartData ->
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
                            productController.uploadProductImages(principal().userId, params.productId, fileNameInServer),
                            HttpStatusCode.OK
                        )
                    )
                }
            }
        }
    }
}