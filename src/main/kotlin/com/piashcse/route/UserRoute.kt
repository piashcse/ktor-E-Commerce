package com.piashcse.route

import com.piashcse.controller.UserController
import com.piashcse.entities.ChangePassword
import com.piashcse.models.user.body.*
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.AppConstants
import com.piashcse.utils.extension.apiResponse
import com.piashcse.utils.sendEmail
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
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
                    userController.login(requestBody),
                    HttpStatusCode.OK
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
            val requiredParams = listOf("email")
            requiredParams.filterNot { call.parameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (email) = requiredParams.map { call.parameters[it]!! }
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
            val requiredParams = listOf("email", "verificationCode", "newPassword")
            requiredParams.filterNot { call.parameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (email, verificationCode, newPassword) = requiredParams.map { call.parameters[it]!! }

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
                val requiredParams = listOf("oldPassword", "newPassword")
                requiredParams.filterNot { call.parameters.contains(it) }.let {
                    if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
                }
                val (oldPassword, newPassword) = requiredParams.map { call.parameters[it]!! }
                val loginUser = call.principal<JwtTokenBody>()
                userController.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword))?.let {
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