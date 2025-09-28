package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
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
fun Route.authRoutes(authController: AuthService) {
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
            // Extract user agent and IP address from the request
            val userAgent = call.request.headers["User-Agent"]
            // Use the local address as fallback since origin may have name conflicts
            val clientIP = call.request.local.remoteAddress
            
            // Create updated request with client information
            val loginRequest = LoginRequest(
                email = requestBody.email,
                password = requestBody.password,
                userType = requestBody.userType,
                userAgent = userAgent,
                ipAddress = clientIP
            )
            
            call.respond(
                ApiResponse.success(
                    authController.login(loginRequest), HttpStatusCode.OK
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
         * Handles the otp-verification request.
         *
         * Receives otp as a query parameter and verifies the opt.
         */
        get("otp-verification", {
            tags("Auth")
            request {
                queryParameter<String>("userId") {
                    required = true
                }
                queryParameter<String>("otp") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (userId, otp) = call.requiredParameters("userId", "otp") ?: return@get
            call.respond(
                ApiResponse.success(
                    authController.otpVerification(userId, otp), HttpStatusCode.OK
                )
            )
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
                queryParameter<String>("userType") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email, userType) = call.requiredParameters("email", "userType") ?: return@get
            val requestBody = ForgetPasswordRequest(email, userType)
            authController.forgetPassword(requestBody).let { otp ->
                sendEmail(requestBody.email, otp)
                call.respond(
                    ApiResponse.success(
                        "${Message.VERIFICATION_CODE_SENT_TO} ${requestBody.email}",
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
                queryParameter<String>("userType") {
                    required = true
                }
            }
            apiResponse()
        }) {
            val (email, otp, newPassword, userType) = call.requiredParameters(
                "email", "otp", "newPassword", "userType"
            ) ?: return@get

            AuthService().resetPassword(
                ResetRequest(
                    email, otp, newPassword, userType
                )
            ).let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        call.respond(
                            ApiResponse.success(
                                Message.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                            )
                        )
                    }

                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        call.respond(
                            ApiResponse.success(
                                Message.VERIFICATION_CODE_IS_NOT_VALID,
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

        /** 
         * Refreshes the access token using a refresh token.
         *
         * Receives a [RefreshTokenRequest] object and responds with a new access token.
         */
        post("refresh", {
            tags("Auth")
            request {
                body<RefreshTokenRequest>()
            }
            apiResponse()
        }) {
            val requestBody = call.receive<RefreshTokenRequest>()
            try {
                call.respond(
                    ApiResponse.success(
                        authController.refreshToken(requestBody), HttpStatusCode.OK
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    ApiResponse.failure(
                        e.message ?: "Invalid refresh token", HttpStatusCode.Unauthorized
                    )
                )
            }
        }

        /** 
         * Logs out the user by invalidating the refresh token.
         *
         * Requires a valid JWT token for authentication.
         */
        authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
            post("logout", {
                tags("Auth")
                protected = true
                apiResponse()
            }) {
                val loginUser = call.principal<JwtTokenRequest>()
                // Get refresh token from header, query parameter, or body
                val refreshToken = call.request.headers["Refresh-Token"] ?: 
                                  call.request.queryParameters["refreshToken"] ?: 
                                  run { /* Try to get from body if needed */ null }
                authController.logout(loginUser?.userId!!, refreshToken).let {
                    call.respond(
                        ApiResponse.success(
                            "Successfully logged out", HttpStatusCode.OK
                        )
                    )
                }
            }
        }
    }
}