package com.piashcse.feature.profile

import com.piashcse.constants.AppConstants
import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.fileExtension
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

/**
 * Route for managing user profile-related operations.
 *
 * @param userProfileController The controller responsible for handling user profile-related operations.
 */
fun Route.profileRoutes(userProfileController: ProfileService) {
    authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
        route("/profile") {

            /**
             * @tag Profile
             * @response 200 [Response]
             */
            get {
                call.respond(
                    ApiResponse.success(
                        userProfileController.getProfile(call.currentUser().userId), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Profile
             * @query firstName
             * @query lastName
             * @query mobile
             * @query faxNumber
             * @query streetAddress
             * @query city
             * @query identificationType
             * @query identificationNo
             * @query occupation
             * @query postCode
             * @query gender
             * @response 200 [Response]
             */
            put {
                val params = UserProfileRequest(
                    firstName = call.parameters["firstName"],
                    lastName = call.parameters["lastName"],
                    mobile = call.parameters["mobile"],
                    faxNumber = call.parameters["faxNumber"],
                    streetAddress = call.parameters["streetAddress"],
                    city = call.parameters["city"],
                    identificationType = call.parameters["identificationType"],
                    identificationNo = call.parameters["identificationNo"],
                    occupation = call.parameters["occupation"],
                    postCode = call.parameters["postCode"],
                    gender = call.parameters["gender"],
                )
                call.respond(
                    ApiResponse.success(
                        userProfileController.updateProfile(call.currentUser().userId, params), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Profile
             * @response 200 [Response]
             */
            post("image-upload") {
                val multipartData = call.receiveMultipart()

                multipartData.forEachPart { part ->
                    when (part) {
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
                                userProfileController.updateProfileImage(call.currentUser().userId, fileNameInServer)
                                    .let {
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