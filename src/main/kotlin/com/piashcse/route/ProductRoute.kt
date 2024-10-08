package com.piashcse.route

import com.piashcse.controller.ProductController
import com.piashcse.models.product.request.AddProduct
import com.piashcse.models.product.request.ProductWithFilter
import com.piashcse.models.product.request.UpdateProduct
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.getCurrentUser
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

fun Route.productRoute(productController: ProductController) {
    route("product") {
        authenticate(RoleManagement.CUSTOMER.role, RoleManagement.SELLER.role, RoleManagement.ADMIN.role) {
            get("{id}", {
                tags("Product")
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val productId = call.parameters["id"]!!
                call.respond(ApiResponse.success(productController.productDetail(productId), HttpStatusCode.OK))
            }
            get({
                tags("Product")
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                    queryParameter<Long>("offset") {
                        required = true
                    }
                    queryParameter<Double>("maxPrice")
                    queryParameter<Double>("minPrice")
                    queryParameter<String>("categoryId")
                    queryParameter<String>("subCategoryId")
                    queryParameter<String>("brandId")
                }
                apiResponse()
            }) {
                val params = ProductWithFilter(
                    limit = call.request.queryParameters["limit"]?.toInt() ?: 0,
                    offset = call.request.queryParameters["offset"]?.toLong() ?: 0L,
                    maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.request.queryParameters["categoryId"],
                    subCategoryId = call.request.queryParameters["subCategoryId"],
                    brandId = call.request.queryParameters["brandId"],
                )
                call.respond(ApiResponse.success(productController.getProducts(params), HttpStatusCode.OK))
            }
        }
        authenticate(RoleManagement.SELLER.role) {
            get("seller", {
                tags("Product")
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                    queryParameter<Long>("offset") {
                        required = true
                    }
                    queryParameter<Double>("maxPrice")
                    queryParameter<Double>("minPrice")
                    queryParameter<String>("categoryId")
                    queryParameter<String>("subCategoryId")
                    queryParameter<String>("brandId")
                }
                apiResponse()
            }) {
                val params = ProductWithFilter(
                    limit = call.request.queryParameters["limit"]?.toInt() ?: 0,
                    offset = call.request.queryParameters["offset"]?.toLong() ?: 0L,
                    maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.request.queryParameters["categoryId"],
                    subCategoryId = call.request.queryParameters["subCategoryId"],
                    brandId = call.request.queryParameters["brandId"],
                )
                call.respond(
                    ApiResponse.success(
                        productController.getProductById(getCurrentUser().userId, params), HttpStatusCode.OK
                    )
                )
            }
            post({
                tags("Product")
                request {
                    body<AddProduct>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<AddProduct>()
                call.respond(
                    ApiResponse.success(
                        productController.addProduct(getCurrentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }
            put("{id}", {
                tags("Product")
                request {
                    pathParameter<String>("id") {
                        required = true
                    }
                    queryParameter<String>("categoryId")
                    queryParameter<String>("subCategoryId")
                    queryParameter<String>("brandId")
                    queryParameter<String>("productName")
                    queryParameter<String>("productCode")
                    queryParameter<Int>("productQuantity")
                    queryParameter<String>("productDetail")
                    queryParameter<Double>("price")
                    queryParameter<Double>("discountPrice")
                    queryParameter<String>("status")
                    queryParameter<String>("videoLink")
                    queryParameter<String>("mainSlider")
                    queryParameter<String>("hotDeal")
                    queryParameter<String>("bestRated")
                    queryParameter<String>("midSlider")
                    queryParameter<String>("hotNew")
                    queryParameter<String>("trend")
                    queryParameter<String>("buyOneGetOne")
                    queryParameter<String>("imageOne")
                    queryParameter<String>("imageOne")
                }
                apiResponse()
            }) {
                val params = UpdateProduct(
                    categoryId = call.request.queryParameters["categoryId"],
                    subCategoryId = call.request.queryParameters["subCategoryId"],
                    brandId = call.request.queryParameters["brandId"],
                    productName = call.request.queryParameters["productName"],
                    productCode = call.request.queryParameters["productCode"],
                    productQuantity = call.request.queryParameters["productQuantity"]?.toIntOrNull(),
                    productDetail = call.request.queryParameters["productDetail"] ?: "",
                    price = call.request.queryParameters["price"]?.toDoubleOrNull(),
                    discountPrice = call.request.queryParameters["discountPrice"]?.toDoubleOrNull(),
                    status = call.request.queryParameters["status"]?.toIntOrNull(),
                    videoLink = call.request.queryParameters["videoLink"],
                    mainSlider = call.request.queryParameters["mainSlider"],
                    hotDeal = call.request.queryParameters["hotDeal"],
                    bestRated = call.request.queryParameters["bestRated"],
                    midSlider = call.request.queryParameters["midSlider"],
                    hotNew = call.request.queryParameters["hotNew"],
                    trend = call.request.queryParameters["trend"],
                    buyOneGetOne = call.request.queryParameters["buyOneGetOne"],
                    imageOne = call.request.queryParameters["imageOne"],
                    imageTwo = call.request.queryParameters["imageTwo"],
                )
                val productId = call.parameters["id"]!!
                call.respond(
                    ApiResponse.success(
                        productController.updateProduct(getCurrentUser().userId, productId, params), HttpStatusCode.OK
                    )
                )
            }
            delete("{id}", {
                tags("Product")
                request {
                    pathParameter<String>("id")
                }
                apiResponse()
            }) {
                val productId = call.parameters["id"]!!
                call.respond(
                    ApiResponse.success(
                        productController.deleteProduct(getCurrentUser().userId, productId), HttpStatusCode.OK
                    )
                )
            }
            post("photo-upload", {
                tags("Product")
                request {
                    queryParameter<String>("id") {
                        required = true
                    }
                    multipartBody {
                        mediaTypes = setOf(ContentType.MultiPart.FormData)
                        part<File>("image") {
                            mediaTypes = setOf(
                                ContentType.Image.PNG, ContentType.Image.JPEG, ContentType.Image.SVG
                            )
                        }
                    }
                }
                apiResponse()
            }) {
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(
                        ApiResponse.success(
                            "Missing parameters: $it", HttpStatusCode.OK
                        )
                    )
                }
                val (id) = requiredParams.map { call.parameters[it]!! }

                val multipartData = call.receiveMultipart()
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            val fileDescription = part.value
                        }

                        is PartData.FileItem -> {
                            UUID.randomUUID()?.let { imageId ->
                                val fileName = part.originalFileName as String
                                val fileLocation = fileName.let {
                                    "${AppConstants.ImageFolder.PRODUCT_IMAGE_LOCATION}$imageId${it.fileExtension()}"
                                }
                                fileLocation.let {
                                    File(it).writeBytes(withContext(Dispatchers.IO) {
                                        part.streamProvider().readBytes()
                                    })
                                }
                                val fileNameInServer = imageId.toString().plus(fileLocation.fileExtension())
                                productController.uploadProductImage(
                                    getCurrentUser().userId,
                                    id,
                                    fileNameInServer,
                                ).let {
                                    call.respond(
                                        ApiResponse.success(fileNameInServer, HttpStatusCode.OK)
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                    part.dispose()
                }
            }
        }
    }
}