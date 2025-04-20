package com.piashcse.modules.profile.routes

import com.piashcse.modules.profile.controller.ProfileController
import com.piashcse.database.models.user.body.UserProfileRequest
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.currentUser
import com.piashcse.utils.extension.fileExtension
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
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
fun Route.profileRoutes(userProfileController: ProfileController) {
    authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
        route("profile") {

            /**
             * GET request to retrieve the profile of the current user.
             *
             * @response A response containing the profile information of the user.
             */
            get({
                tags("Profile")
                summary = "auth[admin, seller, customer]"
                apiResponse()
            }) {
                call.respond(
                    ApiResponse.success(
                        userProfileController.getProfile(call.currentUser().userId), HttpStatusCode.OK
                    )
                )
            }

            /**
             * PUT request to update the profile of the current user.
             *
             * @param firstName The user's first name.
             * @param lastName The user's last name.
             * @param mobile The user's secondary mobile number.
             * @param faxNumber The user's fax number.
             * @param streetAddress The user's street address.
             * @param city The user's city.
             * @param identificationType The type of identification provided.
             * @param identificationNo The identification number.
             * @param occupation The user's occupation.
             * @param postCode The user's postal code.
             * @param gender The user's gender.
             * @response A response indicating the success of the profile update.
             */
            put({
                tags("Profile")
                summary = "auth[admin, seller, customer]"
                request {
                    queryParameter<String>("firstName")
                    queryParameter<String>("lastName")
                    queryParameter<String>("mobile")
                    queryParameter<String>("faxNumber")
                    queryParameter<String>("streetAddress")
                    queryParameter<String>("city")
                    queryParameter<String>("identificationType")
                    queryParameter<String>("identificationNo")
                    queryParameter<String>("occupation")
                    queryParameter<String>("postCode")
                    queryParameter<String>("gender")
                }
                apiResponse()
            }) {
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
             * @param image The image file to be uploaded.
             * @response A response containing the file name of the uploaded image.
             */
            post("image-upload", {
                tags("Profile")
                summary = "auth[admin, seller, customer]"
                request {
                    multipartBody {
                        mediaTypes = setOf(ContentType.MultiPart.FormData)
                        part<File>("image") {
                            mediaTypes = setOf(
                                ContentType.Image.PNG, ContentType.Image.JPEG, ContentType.Image.SVG
                            )
                        }
                    }

                }
                apiResponse()
            }) {
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