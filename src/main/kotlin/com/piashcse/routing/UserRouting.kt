package com.piashcse.routing

import com.piashcse.controller.UserController
import com.piashcse.entities.user.ChangePassword
import com.piashcse.entities.user.UsersEntity
import com.piashcse.models.user.body.*
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.*
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.piashcse.utils.ApiResponse
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail

fun NormalOpenAPIRoute.userRoute(userController: UserController) {
    route("user/") {
        route("login").get<LoginBody, Response> { loginBody ->
            loginBody.validation()
            respond(ApiResponse.success(userController.login(loginBody), HttpStatusCode.OK))
        }
        route("registration").post<Unit, Response, RegistrationBody> { _, registrationBody ->
            registrationBody.validation()
            respond(ApiResponse.success(userController.registration(registrationBody), HttpStatusCode.OK))

        }
        route("forget-password").post<Unit, Response, ForgetPasswordBody> { _, forgetPasswordBody ->
            forgetPasswordBody.validation()
            userController.forgetPassword(forgetPasswordBody).let {
                SimpleEmail().apply {
                    hostName = AppConstants.SmtpServer.HOST_NAME
                    setSmtpPort(AppConstants.SmtpServer.PORT)
                    setAuthenticator(
                        DefaultAuthenticator(
                            AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR,
                            AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR_PASSWORD
                        )
                    )
                    isSSLOnConnect = true
                    setFrom("piash599@gmail.com")
                    subject = AppConstants.SmtpServer.EMAIL_SUBJECT
                    setMsg("Your verification code is : ${it.verificationCode}")
                    addTo(forgetPasswordBody.email)
                    send()
                }
                respond(
                    ApiResponse.success(
                        "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SEND_TO} ${forgetPasswordBody.email}",
                        HttpStatusCode.OK
                    )
                )
            }
        }
        route("verify-password-change").post<Unit, Response, ConfirmPasswordBody> { _, confirmPasswordBody ->
            confirmPasswordBody.validation()
            UserController().confirmPassword(confirmPasswordBody).let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        respond(
                            ApiResponse.success(
                                AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                            )
                        )
                    }
                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        respond(
                            ApiResponse.success(
                                AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                                HttpStatusCode.OK
                            )
                        )
                    }
                }
            }
        }
        authenticateWithJwt(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.USER.role) {
            route("change-password").put<Unit, Response, ChangePassword, JwtTokenBody> { _, requestBody ->
                userController.changePassword(principal().userId, requestBody)?.let {
                    if (it is UsersEntity) respond(
                        ApiResponse.success(
                            "Password hase been changed", HttpStatusCode.OK
                        )
                    )
                    if (it is ChangePassword) respond(
                        ApiResponse.failure(
                            "Old password is wrong", HttpStatusCode.OK
                        )
                    )
                } ?: run {
                    throw UserNotExistException()
                }
            }

            route("profile").get<Unit, Response, JwtTokenBody> { _ ->
                respond(
                    ApiResponse.success(
                        userController.getProfile(principal().userId), HttpStatusCode.OK
                    )
                )
            }
            route("profile").put<Unit, Response, UserProfileBody, JwtTokenBody> { params, requestBody ->
                respond(
                    ApiResponse.success(
                        userController.updateProfile(principal().userId, requestBody), HttpStatusCode.OK
                    )
                )
            }

            /*route("photo-upload").put<UserId, Response, MultipartImage, JwtTokenBody> { params, multipartData ->
                params.validation()
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
                    userController.updateProfileImage(params.userId, fileNameInServer)?.let {
                        respond(
                            ApiResponse.success(fileNameInServer, HttpStatusCode.OK)
                        )
                    }
                }
            }*/
        }
    }
}
