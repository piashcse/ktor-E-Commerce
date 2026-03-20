package com.piashcse.feature.profile

import com.piashcse.constants.AppConstants
import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.currentUserId
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
    authenticate(
        RoleManagement.SUPER_ADMIN.role,
        RoleManagement.ADMIN.role,
        RoleManagement.SELLER.role,
        RoleManagement.CUSTOMER.role
    ) {
        route("/profile") {

            /**
             * @tag Profile
             * @description Retrieve the authenticated user's profile information
             * @operationId getUserProfile
             * @response 200 User profile retrieved successfully
             * @security jwtToken
             */
            get {
                call.respond(
                    ApiResponse.success(
                        userProfileController.getProfile(call.currentUserId), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Profile
             * @description Update the authenticated user's profile information
             * @operationId updateUserProfile
             * @query firstName User's first name
             * @query lastName User's last name
             * @query mobile Mobile phone number
             * @query faxNumber Fax number
             * @query streetAddress Street address
             * @query city City of residence
             * @query identificationType Type of identification document
             * @query identificationNo Identification document number
             * @query occupation User's occupation
             * @query postCode Postal/ZIP code
             * @query gender User's gender
             * @response 200 User profile updated successfully
             * @security jwtToken
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
                        userProfileController.updateProfile(call.currentUserId, params), HttpStatusCode.OK
                    )
                )
            }

            /**
             * @tag Profile
             * @description Upload a new profile image for the authenticated user
             * @operationId uploadProfileImage
             * @form image (required) Profile image file to upload
             * @response 200 Profile image uploaded successfully
             * @security jwtToken
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
                                userProfileController.updateProfileImage(call.currentUserId, fileNameInServer)
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