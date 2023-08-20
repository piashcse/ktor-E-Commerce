package com.piashcse.routing

import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.piashcse.controller.ProfileController
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.models.user.body.MultipartImage
import com.piashcse.models.user.body.UserId
import com.piashcse.models.user.body.UserProfileBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.piashcse.utils.extension.fileExtension
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

fun NormalOpenAPIRoute.profileRouting(profileController: ProfileController) {
    authenticateWithJwt(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.USER.role) {
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

        route("user-photo-upload").post<Unit, Response, MultipartImage, JwtTokenBody> { _, multipartData ->
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
    }
}