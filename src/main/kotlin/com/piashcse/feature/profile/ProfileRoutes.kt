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
        route("profile") {

            /**
             * GET request to retrieve the profile of the current user.
             *
             * @tag Profile
             * @summary auth[admin, seller, customer]
             * @response 200 [ApiResponse] A response containing the profile information of the user.
             */
            get("/profile") {
                call.respond(
                    ApiResponse.success(
                        userProfileController.getProfile(call.currentUser().userId), HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to update the profile of the current user.
             *
             * @tag Profile
             * @summary auth[admin, seller, customer]
             * @query firstName The user's first name.
             * @query lastName The user's last name.
             * @query mobile The user's secondary mobile number.
             * @query faxNumber The user's fax number.
             * @query streetAddress The user's street address.
             * @query city The user's city.
             * @query identificationType The type of identification provided.
             * @query identificationNo The identification number.
             * @query occupation The user's occupation.
             * @query postCode The user's postal code.
             * @query gender The user's gender.
             * @response 200 [ApiResponse] A response indicating the success of the profile update.
             */
            put("/profile") {
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
             * POST request to upload a new profile image for the user.
             *
             * @tag Profile
             * @summary auth[admin, seller, customer]
             * @response 200 [ApiResponse] A response containing the file name of the uploaded image.
             */
            post("/profile/image-upload") {
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