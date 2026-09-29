package com.piashcse.feature.profile

import com.piashcse.constants.Message
import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.writeRateLimit
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * User profile management routes.
 */
fun Route.profileRoutes() {
    val userProfileService: ProfileService by inject()
    customerOnlyAuth {
        /**
         * @tag Profile
         * @description Retrieve the authenticated user's profile information
         */
        get {
            call.respondOk(userProfileService.getProfile(call.currentUserId))
        }

        writeRateLimit {
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
                call.respondOk(userProfileService.updateProfile(call.currentUserId, params))
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
                        // Size cap (5 MB) + MIME allowlist enforced before the upload call.
                        val bytes = UploadService.readAndValidateImagePart(part, "profile image")
                        val fileName = UploadService.uploadProfileImage(part, bytes)
                        imageUrl = UploadService.getProfileImageUrl(fileName)
                        userProfileService.updateProfileImage(call.currentUserId, imageUrl)
                    }
                    part.dispose()
                }

                call.respondOk(imageUrl ?: throw ValidationException(Message.Validation.FILE_REQUIRED))
            }
        }
    }
}
