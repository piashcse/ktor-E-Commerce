package com.piashcse.route

import com.piashcse.controller.UserController
import com.piashcse.entities.ChangePassword
import com.piashcse.models.user.body.*
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.extension.requiredParameters
import com.piashcse.utils.sendEmail
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoute(userController: UserController) {
    route("user") {
        post("Login", {
            tags("User")
            request {
                body<LoginBody>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<LoginBody>()
            call.respond(
                ApiResponse.success(
                    userController.login(requestBody), HttpStatusCode.OK
                )
            )
        }
        post("registration", {
            tags("User")
            request {
                body<RegistrationBody>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<RegistrationBody>()
            requestBody.validation()
            call.respond(ApiResponse.success(userController.addUser(requestBody), HttpStatusCode.OK))
        }
        get("forget-password", {
            tags("User")
            request {
                queryParameter<String>("email") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email) = call.requiredParameters("email") ?: return@get
            val requestBody = ForgetPasswordEmail(email)
            userController.forgetPasswordSendCode(requestBody).let {
                sendEmail(requestBody.email, it.verificationCode)
                call.respond(

                    ApiResponse.success(
                        "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SENT_TO} ${requestBody.email}",
                        HttpStatusCode.OK
                    )
                )
            }
        }
        get("verify-password-change", {
            tags("User")
            request {
                queryParameter<String>("email") {
                    required = true
                }
                queryParameter<String>("verificationCode") {
                    required = true
                }
                queryParameter<String>("newPassword") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email, verificationCode, newPassword) = call.requiredParameters(
                "email", "verificationCode", "newPassword"
            ) ?: return@get

            UserController().forgetPasswordVerificationCode(
                ConfirmPassword(
                    email, verificationCode, newPassword
                )
            ).let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        call.respond(
                            ApiResponse.success(
                                AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                            )
                        )
                    }

                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        call.respond(
                            ApiResponse.success(
                                AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                                HttpStatusCode.OK
                            )
                        )
                    }
                }
            }
        }
        authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
            put("change-password", {
                tags("User")
                protected = true
                request {
                    queryParameter<String>("oldPassword") {
                        required = true
                    }
                    queryParameter<String>("newPassword") {
                        required = true
                    }
                }
                apiResponse()
            }) {
                val (oldPassword, newPassword) = call.requiredParameters("oldPassword", "newPassword") ?: return@put
                val loginUser = call.principal<JwtTokenBody>()
                userController.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword)).let {
                    if (it) call.respond(
                        ApiResponse.success(
                            "Password has been changed", HttpStatusCode.OK
                        )
                    ) else call.respond(
                        ApiResponse.failure(
                            "Old password is wrong", HttpStatusCode.OK
                        )
                    )
                }
            }
        }
    }
}