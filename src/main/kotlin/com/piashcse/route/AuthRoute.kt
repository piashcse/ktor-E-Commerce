package com.piashcse.route

import com.piashcse.controller.AuthController
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

/**
 * Defines authentication routes for login, registration, password reset, and password change.
 *
 * @param authController The controller handling authentication-related operations.
 */
fun Route.authRoute(authController: AuthController) {
    route("auth") {
        /**
         * Handles the login request.
         *
         * Receives a [LoginRequest] object and responds with a successful login response.
         */
        post("login", {
            tags("Auth")
            request {
                body<LoginRequest>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<LoginRequest>()
            call.respond(
                ApiResponse.success(
                    authController.login(requestBody), HttpStatusCode.OK
                )
            )
        }

        /**
         * Handles the registration request.
         *
         * Receives a [RegisterRequest] object and responds with a successful registration response.
         */
        post("register", {
            tags("Auth")
            request {
                body<RegisterRequest>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<RegisterRequest>()
            call.respond(ApiResponse.success(authController.register(requestBody), HttpStatusCode.OK))
        }

        /**
         * Handles the request for sending a password reset verification code.
         *
         * Receives the user's email as a query parameter and sends a verification code to the email.
         */
        get("forget-password", {
            tags("Auth")
            request {
                queryParameter<String>("email") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email) = call.requiredParameters("email") ?: return@get
            val requestBody = ForgetPasswordRequest(email)
            authController.forgetPassword(requestBody).let { otp ->
                sendEmail(requestBody.email, otp)
                call.respond(
                    ApiResponse.success(
                        "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SENT_TO} ${requestBody.email}",
                        HttpStatusCode.OK
                    )
                )
            }
        }

        /**
         * Handles the request for resetting the password.
         *
         * Receives the email, OTP, and new password as query parameters and verifies the password reset code.
         */
        get("reset-password", {
            tags("Auth")
            request {
                queryParameter<String>("email") {
                    required = true
                }
                queryParameter<String>("otp") {
                    required = true
                }
                queryParameter<String>("newPassword") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email, verificationCode, newPassword) = call.requiredParameters(
                "email", "otp", "newPassword"
            ) ?: return@get

            AuthController().resetPassword(
                ResetRequest(
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

        /**
         * Handles the request to change the password for authenticated users.
         *
         * Requires the old and new password as query parameters and responds with a success or failure message.
         */
        authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
            put("change-password", {
                tags("Auth")
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
                val loginUser = call.principal<JwtTokenRequest>()
                authController.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword)).let {
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