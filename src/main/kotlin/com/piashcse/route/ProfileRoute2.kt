package com.piashcse.route

import com.piashcse.controller.ProfileController
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.models.user.body.UserProfileBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.Response
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

fun Route.profileRouting(profileController: ProfileController) {
    authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
        get("profile", {
            tags("User")
            response {
                HttpStatusCode.OK to {
                    description = "Successful"
                    body<Response> {
                        mediaTypes = setOf(ContentType.Application.Json)
                        description = "Successful"
                    }
                }
                HttpStatusCode.InternalServerError
            }
        }) {
            val loginUser = call.principal<JwtTokenBody>()
            call.respond(
                ApiResponse.success(
                    profileController.getProfile(loginUser?.userId!!), HttpStatusCode.OK
                )
            )
        }
        put("profile", {
            tags("User")
            request {
                body<UserProfileBody>()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful"
                    body<Response> {
                        mediaTypes = setOf(ContentType.Application.Json)
                        description = "Successful"
                    }
                }
                HttpStatusCode.InternalServerError
            }
        }) {

            val loginUser = call.principal<JwtTokenBody>()
            val requestBody = call.receive<UserProfileBody>()
            call.respond(
                ApiResponse.success(
                    profileController.updateProfile(loginUser?.userId !!, requestBody), HttpStatusCode.OK
                )
            )
        }

        post("profile", {
            tags("User")
            response {
                HttpStatusCode.OK to {
                    description = "Successful"
                    body<Response> {
                        mediaTypes = setOf(ContentType.Application.Json)
                        description = "Successful"
                    }
                }
                HttpStatusCode.InternalServerError
            }
        }) {
            val loginUser = call.principal<JwtTokenBody>()
            val multipart = call.receiveMultipart()

            UUID.randomUUID()?.let { imageId ->
               /* val fileLocation = multipartData.file.name?.let {
                    "${AppConstants.Image.PROFILE_IMAGE_LOCATION}$imageId${it.fileExtension()}"
                }
                fileLocation?.let {
                  *//* *//**//* File(it).writeBytes(withContext(Dispatchers.IO) {
                        multipartData.file.readAllBytes()
                    })*//*
                }
                val fileNameInServer = imageId.toString().plus(fileLocation?.fileExtension())
                profileController.updateProfileImage(loginUser?.userId !!, fileNameInServer)?.let {
                    call.respond(
                        ApiResponse.success(fileNameInServer, HttpStatusCode.OK)
                    )
                }*/
            }
        }
    }
}

/*
route("profile").get<Unit, Response, JwtTokenBody> { _ ->
            respond(
                ApiResponse.success(
                    profileController.getProfile(principal().userId), HttpStatusCode.OK
                )
            )
        }
        route("profile").put<Unit, Response, UserProfileBody, JwtTokenBody> { _, requestBody ->
            respond(
                ApiResponse.success(
                    profileController.updateProfile(principal().userId, requestBody), HttpStatusCode.OK
                )
            )
        }

        route("profile-photo-upload").post<Unit, Response, MultipartImage, JwtTokenBody> { _, multipartData ->
            multipartData.validation()

            UUID.randomUUID()?.let { imageId ->
                val fileLocation = multipartData.file.name?.let {
                    "${AppConstants.Image.PROFILE_IMAGE_LOCATION}$imageId${it.fileExtension()}"
                }
                fileLocation?.let {
                    File(it).writeBytes(withContext(Dispatchers.IO) {
                        multipartData.file.readAllBytes()
                    })
                }
                val fileNameInServer = imageId.toString().plus(fileLocation?.fileExtension())
                profileController.updateProfileImage(principal().userId, fileNameInServer)?.let {
                    respond(
                        ApiResponse.success(fileNameInServer, HttpStatusCode.OK)
                    )
                }
            }
        }
 */
