package com.piashcse.feature.profile

import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.RoleManagement
import com.piashcse.service.UploadService
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.currentUserId
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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

            /**
             * @tag Profile
             * @description Retrieve the authenticated user's profile information
             * @operationId getUserProfile
             * @response 200 User profile retrieved successfully
             * @security jwtToken
             */
            get {
                call.respond(
                    HttpStatusCode.OK,
                    userProfileController.getProfile(call.currentUserId)
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
                    HttpStatusCode.OK,
                    userProfileController.updateProfile(call.currentUserId, params)
                )
            }

            /**
             * @tag Profile
             * @description Upload a profile image (JPG, PNG, WebP, GIF - max 5MB)
             * @operationId uploadProfileImage
             * @form image (required) Profile image file
             * @response 200 Returns image URL
             * @security jwtToken
             */
            post("image-upload") {
                val multipart = call.receiveMultipart()
                var imageUrl: String? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val fileName = UploadService.uploadProfileImage(part)
                        imageUrl = UploadService.getProfileImageUrl(fileName)
                        userProfileController.updateProfileImage(call.currentUserId, imageUrl)
                    }
                    part.dispose()
                }

                call.respond(HttpStatusCode.OK, imageUrl ?: throw ValidationException("No file uploaded"))
            }
        }
    }
