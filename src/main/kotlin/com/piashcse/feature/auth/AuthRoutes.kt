package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.requireRole
import com.piashcse.utils.email.sendEmail
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.requireParameters
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.MissingParameterException
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Authentication and registration routes.
 */
fun Route.authRoutes(authService: AuthService) {
    // Rate-limited endpoints (brute-force protection)
    rateLimit(RateLimitName(RateLimitNames.AUTH)) {
        /**
         * @tag Auth
         * @description Authenticate user with email, password and user type
         */
        post("login") {
            val requestBody = call.receive<LoginRequest>()
            call.respond(HttpStatusCode.OK, authService.login(requestBody))
        }

        /**
         * @tag Auth
         * @description Register a new user account
         */
        post("register") {
            val requestBody = call.receive<RegisterRequest>()
            call.respond(HttpStatusCode.OK, authService.register(requestBody))
        }

        /**
         * @tag Auth
         * @description Request password reset OTP
         */
        post("forget-password") {
            val requestBody = call.receive<ForgetPasswordRequest>()
            authService.forgetPassword(requestBody).let { otp ->
                sendEmail(requestBody.email, otp)
                call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.OTP_SENT))
            }
        }

        /**
         * @tag Auth
         * @description Reset password using OTP verification
         */
        post("reset-password") {
            val requestBody = call.receive<ResetRequest>()
            requestBody.validation()

            authService.resetPassword(requestBody).let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                    }
                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.OTP_INVALID))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError, mapOf<String, String>("message" to Message.Errors.INTERNAL))
                }
            }
        }
    }

    /**
     * @tag Auth
     * @description Verify user account with OTP
     */
    get("otp-verification") {
        val (userId, otp) = call.requireParameters("userId", "otp")
        call.respond(HttpStatusCode.OK, authService.otpVerification(userId, otp))
    }

    /**
     * @tag Auth
     * @description Refresh access token using refresh token
     */
    post("refresh-token") {
        val requestBody = call.receive<RefreshTokenRequest>()
        val tokenPair = authService.refreshAccessToken(requestBody)
        call.respond(HttpStatusCode.OK, tokenPair)
    }

    requireRole {
        /**
         * @tag Auth
         * @description Logout authenticated user
         */
        post("logout") {
            val userId = call.currentUserId
            val requestBody = call.receive<LogoutRequest>()
            authService.logout(userId, requestBody.refreshToken)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
        }

        /**
         * @tag Auth
         * @description Change password for authenticated user
         */
        put("change-password") {
            val (oldPassword, newPassword) = call.requireParameters("oldPassword", "newPassword")
            val loginUser = call.principal<JwtTokenRequest>()
            authService.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword)).let {
                if (it) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("message" to Message.Auth.INVALID_CREDENTIALS))
                }
            }
        }
    }
}

/**
 * Administrative authentication/user management routes.
 */
fun Route.authAdminRoutes(authService: AuthService) {
    /**
     * @tag Auth
     * @description Admin: Change user type
     */
    put("/{userId}/change-user-type") {
        val (userId) = call.requireParameters("userId")
        val userTypeParam =
            call.parameters["userType"]
                ?: throw MissingParameterException("userType")

        val newType =
            try {
                UserType.valueOf(userTypeParam.uppercase())
            } catch (e: IllegalArgumentException) {
                throw InvalidEnumValueException(
                    message = "Invalid userType: $userTypeParam",
                    enumName = UserType.values().joinToString(", ") { it.name },
                    invalidValue = userTypeParam,
                )
            }

        val currentUser = call.principal<JwtTokenRequest>()
        if (authService.changeUserType(currentUser?.userId!!, userId, newType)) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "User type updated successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to update user type"))
        }
    }

    /**
     * @tag Auth
     * @description Admin: Deactivate a user account
     */
    put("/{userId}/deactivate") {
        val (userId) = call.requireParameters("userId")
        val currentUser = call.principal<JwtTokenRequest>()
        if (authService.deactivateUser(currentUser?.userId!!, userId)) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "User deactivated successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to deactivate user"))
        }
    }

    /**
     * @tag Auth
     * @description Admin: Activate a user account
     */
    put("/{userId}/activate") {
        val (userId) = call.requireParameters("userId")
        val currentUser = call.principal<JwtTokenRequest>()
        if (authService.activateUser(currentUser?.userId!!, userId)) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "User activated successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to activate user"))
        }
    }
}
