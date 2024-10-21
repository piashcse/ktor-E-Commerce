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
                    limit = call.parameters["limit"]?.toInt() ?: 0,
                    offset = call.parameters["offset"]?.toLong() ?: 0L,
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
                    queryParameter<Long>("offset") {
                        required = true
                    }
                    queryParameter<String>("productName"){
                        required = true
                    }
                    queryParameter<String>("categoryId")
                    queryParameter<Double>("maxPrice")
                    queryParameter<Double>("minPrice")
                }
                apiResponse()
            }) {
                val queryParams = ProductSearch(
                    limit = call.parameters["limit"]?.toInt() ?: 0,
                    offset = call.parameters["offset"]?.toLong() ?: 0L,
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
                    limit = call.parameters["limit"]?.toInt() ?: 0,
                    offset = call.parameters["offset"]?.toLong() ?: 0L,
                    maxPrice = call.parameters["maxPrice"]?.toDoubleOrNull(),
                    minPrice = call.parameters["minPrice"]?.toDoubleOrNull(),
                    categoryId = call.parameters["categoryId"],
                    subCategoryId = call.parameters["subCategoryId"],
                    brandId = call.parameters["brandId"],
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        productController.updateProduct(getCurrentUser().userId, id, params), HttpStatusCode.OK
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (id) = requiredParams.map { call.parameters[it]!! }
                call.respond(
                    ApiResponse.success(
                        productController.deleteProduct(getCurrentUser().userId, id), HttpStatusCode.OK
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
                val requiredParams = listOf("id")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
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