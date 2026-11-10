@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.profile

import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.writeRateLimit
import com.piashcse.service.UploadService
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.receiveSingleImage
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/**
 * User profile management routes.
 */
fun Route.profileRoutes() {
    val userProfileService: ProfileService by inject()
    customerOnlyAuth {
        /**
         * Retrieve the authenticated user's profile information
         */
        get {
            call.respondOk(userProfileService.getProfile(call.currentUserId))
        }.describe { summary = "Retrieve the authenticated user's profile information" }

        writeRateLimit {
            /**
             * Update the authenticated user's profile information
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
            }.describe { summary = "Update the authenticated user's profile information" }

            /**
             * Upload a profile image
             */
            post("image-upload") {
                val imageUrl =
                    call.receiveSingleImage(
                        purpose = "profile image",
                        upload = UploadService::uploadProfileImage,
                        publicUrl = UploadService::getProfileImageUrl,
                    )
                userProfileService.updateProfileImage(call.currentUserId, imageUrl)
                call.respondOk(imageUrl)
            }.describe { summary = "Upload a profile image" }
        }
    }
}
