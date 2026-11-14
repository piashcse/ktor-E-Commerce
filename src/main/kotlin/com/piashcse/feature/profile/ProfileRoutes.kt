@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.profile

import com.piashcse.model.request.UserProfileRequest
import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.jsonBody
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
                // Prefer a JSON body; keep query parameters working for older clients.
                val body = runCatching { call.receive<UserProfileRequest>() }.getOrNull()

                fun value(
                    fromBody: String?,
                    query: String,
                ): String? = fromBody ?: call.parameters[query]

                val params =
                    UserProfileRequest(
                        firstName = value(body?.firstName, "firstName"),
                        lastName = value(body?.lastName, "lastName"),
                        mobile = value(body?.mobile, "mobile"),
                        faxNumber = value(body?.faxNumber, "faxNumber"),
                        streetAddress = value(body?.streetAddress, "streetAddress"),
                        city = value(body?.city, "city"),
                        identificationType = value(body?.identificationType, "identificationType"),
                        identificationNo = value(body?.identificationNo, "identificationNo"),
                        occupation = value(body?.occupation, "occupation"),
                        postCode = value(body?.postCode, "postCode"),
                        gender = value(body?.gender, "gender"),
                    )
                call.respondOk(userProfileService.updateProfile(call.currentUserId, params))
            }.describe {
                summary = "Update the authenticated user's profile information"
                description = "Accepts a JSON body (preferred) or query parameters (legacy)"
                requestBody { jsonBody<UserProfileRequest>() }
            }

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
