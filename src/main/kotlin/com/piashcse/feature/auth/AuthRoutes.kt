package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.plugin.*
import com.piashcse.utils.ApiError
import com.piashcse.utils.InvalidEnumValueException
import com.piashcse.utils.MissingParameterException
import com.piashcse.utils.UnauthorizedException
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import com.piashcse.utils.sendEmail
import com.piashcse.plugin.RateLimitNames
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines authentication routes for login, registration, password reset, and password change.
 *
 * @param authController The controller handling authentication-related operations.
 */
fun Route.authRoutes(authController: AuthService) {
    // Rate-limited endpoints (brute-force protection)
    rateLimit(RateLimitName(RateLimitNames.AUTH)) {
            /**
             * @tag Auth
             * @description Authenticate user with email, password and user type
             * @operationId login
             * @body LoginRequest
             * @response 200 User authentication successful
             * @response 400 Invalid credentials
             */
            post("login") {
                val requestBody = call.receive<LoginRequest>()
                call.respond(HttpStatusCode.OK, authController.login(requestBody))
            }

            /**
             * @tag Auth
             * @description Register a new user account
             * @operationId register
             * @body RegisterRequest
             * @response 200 User registered successfully
             * @response 400 Invalid registration data
             */
            post("register") {
                val requestBody = call.receive<RegisterRequest>()
                call.respond(HttpStatusCode.OK, authController.register(requestBody))
            }

            /**
             * @tag Auth
             * @description Request password reset OTP
             * @operationId forgetPassword
             * @body ForgetPasswordRequest
             * @response 200 OTP sent successfully
             * @response 400 Invalid email or user type
             */
            post("forget-password") {
                val requestBody = call.receive<ForgetPasswordRequest>()
                authController.forgetPassword(requestBody).let { otp ->
                    sendEmail(requestBody.email, otp)
                    call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.OTP_SENT))
                }
            }

            /**
             * @tag Auth
             * @description Reset password using OTP verification
             * @operationId resetPassword
             * @body ResetRequest
             * @response 200 Password reset successful
             * @response 400 Invalid OTP or email
             */
            post("reset-password") {
                val requestBody = call.receive<ResetRequest>()
                requestBody.validation()

                authController.resetPassword(requestBody).let {
                    when (it) {
                        AppConstants.DataBaseTransaction.FOUND -> {
                            call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                        }

                        AppConstants.DataBaseTransaction.NOT_FOUND -> {
                            call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.OTP_INVALID))
                        }
                    }
                }
            }
        }

        /**
         * @tag Auth
         * @description Verify user account with OTP
         * @operationId verifyOtp
         * @query userId (required) User ID
         * @query otp (required) One-time password
         * @response 200 OTP verified successfully
         * @response 400 Invalid OTP
         */
        get("otp-verification") {
            val (userId, otp) = call.requireParameters("userId", "otp")
            call.respond(HttpStatusCode.OK, authController.otpVerification(userId, otp))
        }

        /**
         * @tag Auth
         * @description Refresh access token using refresh token
         * @operationId refreshToken
         * @body RefreshTokenRequest
         * @response 200 Token refreshed successfully
         * @response 401 Invalid or expired refresh token
         */
        post("refresh-token") {
            val requestBody = call.receive<RefreshTokenRequest>()
            val tokenPair = authController.refreshAccessToken(requestBody)
            call.respond(HttpStatusCode.OK, tokenPair)
        }

        requireRole {
            /**
             * @tag Auth
             * @description Logout authenticated user
             * @operationId logout
             * @body LogoutRequest
             * @response 200 Logged out successfully
             * @security jwtToken
             */
            post("logout") {
                val userId = call.currentUserId
                val requestBody = call.receive<LogoutRequest>()
                authController.logout(userId, requestBody.refreshToken)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
            }

            /**
             * @tag Auth
             * @description Change password for authenticated user
             * @operationId changePassword
             * @query oldPassword (required) Current password
             * @query newPassword (required) New password
             * @response 200 Password changed successfully
             * @response 400 Invalid old password
             * @security jwtToken
             */
            put("change-password") {
                val (oldPassword, newPassword) = call.requireParameters("oldPassword", "newPassword")
                val loginUser = call.principal<JwtTokenRequest>()
                authController.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword)).let {
                    if (it) call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                    else call.respond(HttpStatusCode.Unauthorized, mapOf("message" to Message.Auth.INVALID_CREDENTIALS))
                }
            }
        }

        adminAuth {
            /**
             * @tag Auth
             * @description Change user type (Admin/Super Admin only)
             * @operationId changeUserType
             * @path userId (required) User ID to update
             * @query userType (required) New user type
             * @response 200 User type changed successfully
             * @response 400 Invalid user type
             * @security jwtToken
             */
            put("/{userId}/change-user-type") {
                val (userId) = call.requireParameters("userId")
                val userTypeParam = call.parameters["userType"]
                    ?: throw MissingParameterException("userType")

                val newType = try {
                    UserType.valueOf(userTypeParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    throw InvalidEnumValueException(
                        message = "Invalid userType: $userTypeParam",
                        enumName = UserType.values().joinToString(", ") { it.name },
                        invalidValue = userTypeParam
                    )
                }

                val currentUser = call.principal<JwtTokenRequest>()
                    ?: throw UnauthorizedException()

                val success = authController.changeUserType(
                    currentUser.userId,
                    userId,
                    newType
                )

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User type changed successfully to $newType"))
                } else {
                    throw ValidationException("Failed to change user type")
                }
            }

            /**
             * @tag Auth
             * @description Deactivate a user account (Admin/Super Admin only)
             * @operationId deactivateUser
             * @path userId (required) User ID to deactivate
             * @response 200 User deactivated successfully
             * @response 400 Cannot deactivate user
             * @security jwtToken
             */
            put("/{userId}/deactivate") {
                val (userId) = call.requireParameters("userId")
                val currentUser = call.principal<JwtTokenRequest>()
                    ?: throw UnauthorizedException()

                val success = authController.deactivateUser(currentUser.userId, userId)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User deactivated successfully"))
                } else {
                    throw ValidationException("Failed to deactivate user")
                }
            }

            /**
             * @tag Auth
             * @description Activate a previously deactivated user account
             * @operationId activateUser
             * @path userId (required) User ID to activate
             * @response 200 User activated successfully
             * @response 400 Cannot activate user
             * @security jwtToken
             */
            put("/{userId}/activate") {
                val (userId) = call.requireParameters("userId")
                val currentUser = call.principal<JwtTokenRequest>()
                    ?: throw UnauthorizedException()

                val success = authController.activateUser(currentUser.userId, userId)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "User activated successfully"))
                } else {
                    throw ValidationException("Failed to activate user")
                }
        }
    }
}
