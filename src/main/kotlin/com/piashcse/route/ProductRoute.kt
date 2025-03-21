package com.piashcse.route

import com.piashcse.controller.ProductController
import com.piashcse.models.product.request.AddProduct
import com.piashcse.models.product.request.ProductSearch
import com.piashcse.models.product.request.ProductWithFilter
import com.piashcse.models.product.request.UpdateProduct
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.requiredParameters
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.http.content.*
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
                val (productId) = call.requiredParameters("productId") ?: return@get
                call.respond(ApiResponse.success(productController.productDetail(productId), HttpStatusCode.OK))
            }
            get({
                tags("Product")
                request {
                    queryParameter<Int>("limit") {
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
                val (limit) = call.requiredParameters("limit") ?: return@get
                val params = ProductWithFilter(
                    limit = limit.toInt(),
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"],
                )
                call.respond(ApiResponse.success(productController.getProducts(params), HttpStatusCode.OK))
            }

            get("search", {
                tags("Product")
                request {
                    queryParameter<Int>("limit") {
                        required = true
                    }
                    queryParameter<String>("productName") {
                        required = true
                    }
                    queryParameter<String>("categoryId")
                    queryParameter<Double>("maxPrice")
                    queryParameter<Double>("minPrice")
                }
                apiResponse()
            }) {
                val (limit) = call.requiredParameters("limit") ?: return@get
                val queryParams = ProductSearch(
                    limit = limit.toInt(),
                    productName = call.parameters["productName"]!!,
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                )
                call.respond(ApiResponse.success(productController.searchProduct(queryParams), HttpStatusCode.OK))
            }

        }
        authenticate(RoleManagement.SELLER.role) {
            get("seller", {
                tags("Product")
                request {
                    queryParameter<Int>("limit") {
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
                val (limit) = call.requiredParameters("limit") ?: return@get
                val params = ProductWithFilter(
                    limit = limit.toInt(),
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"],
                )
                call.respond(
                    ApiResponse.success(
                        productController.getProductById(call.currentUser().userId, params), HttpStatusCode.OK
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
                        productController.addProduct(call.currentUser().userId, requestBody), HttpStatusCode.OK
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
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"],
                    productName = call.parameters["productName"],
                    productCode = call.parameters["productCode"],
                    productQuantity = call.parameters["productQuantity"]?.toIntOrNull(),
                    productDetail = call.parameters["productDetail"] ?: "",
                    price = call.parameters["price"]?.toDoubleOrNull(),
                    discountPrice = call.parameters["discountPrice"]?.toDoubleOrNull(),
                    status = call.parameters["status"]?.toIntOrNull(),
                    videoLink = call.parameters["videoLink"],
                    mainSlider = call.parameters["mainSlider"],
                    hotDeal = call.parameters["hotDeal"],
                    bestRated = call.parameters["bestRated"],
                    midSlider = call.parameters["midSlider"],
                    hotNew = call.parameters["hotNew"],
                    trend = call.parameters["trend"],
                    buyOneGetOne = call.parameters["buyOneGetOne"],
                    imageOne = call.parameters["imageOne"],
                    imageTwo = call.parameters["imageTwo"],
                )
                val (id) = call.requiredParameters("id") ?: return@put
                call.respond(
                    ApiResponse.success(
                        productController.updateProduct(call.currentUser().userId, id, params), HttpStatusCode.OK
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
                val (id) = call.requiredParameters("id") ?: return@delete
                call.respond(
                    ApiResponse.success(
                        productController.deleteProduct(call.currentUser().userId, id), HttpStatusCode.OK
                    )
                )
            }
            post("image-upload", {
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
                val (id) = call.requiredParameters("id") ?: return@post
                val multipartData = call.receiveMultipart()
                multipartData.forEachPart { part ->
                    when (part) {
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
                                    call.currentUser().userId,
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