package com.piashcse.feature.profile

import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.requireRole
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * User profile management routes.
 */
fun Route.profileRoutes(userProfileService: ProfileService) {
    requireRole {
        /**
         * @tag Profile
         * @description Retrieve the authenticated user's profile information
         */
        get {
            call.respond(
                HttpStatusCode.OK,
                userProfileService.getProfile(call.currentUserId),
            )
        }

        /**
         * @tag Profile
         * @description Update the authenticated user's profile information
         */
        put {
            val params =
                UserProfileRequest(
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
                userProfileService.updateProfile(call.currentUserId, params),
            )
        }

        /**
         * @tag Profile
         * @description Upload a profile image
         */
        post("image-upload") {
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val fileName = UploadService.uploadProfileImage(part)
                    imageUrl = UploadService.getProfileImageUrl(fileName)
                    userProfileService.updateProfileImage(call.currentUserId, imageUrl)
                }
                part.dispose()
            }

            call.respond(HttpStatusCode.OK, imageUrl ?: throw ValidationException("No file uploaded"))
        }
    }
}
