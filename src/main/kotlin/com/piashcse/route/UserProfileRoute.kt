package com.piashcse.route

import com.piashcse.controller.userProfileController
import com.piashcse.models.user.body.UserProfileBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.fileExtension
import com.piashcse.utils.extension.getCurrentUser
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

fun Route.userProfileRoute(userProfileController: userProfileController) {
    authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
        route("user") {
            get({
                tags("User")
                apiResponse()
            }) {
                call.respond(
                    ApiResponse.success(
                        userProfileController.getProfile(getCurrentUser().userId), HttpStatusCode.OK
                    )
                )
            }
            put({
                tags("User")
                request {
                    body<UserProfileBody>()
                }
                apiResponse()
            }) {
                val requestBody = call.receive<UserProfileBody>()
                call.respond(
                    ApiResponse.success(
                        userProfileController.updateProfile(getCurrentUser().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }

            post("photo-upload",{
                tags("User")
                request {
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
                                    "${AppConstants.ImageFolder.PROFILE_IMAGE_LOCATION}$imageId${it.fileExtension()}"
                                }
                                fileLocation.let {
                                    File(it).writeBytes(withContext(Dispatchers.IO) {
                                        part.streamProvider().readBytes()
                                    })
                                }
                                val fileNameInServer = imageId.toString().plus(fileLocation.fileExtension())
                                userProfileController.updateProfileImage(getCurrentUser().userId, fileNameInServer)?.let {
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