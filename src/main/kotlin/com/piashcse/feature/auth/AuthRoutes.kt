package com.piashcse.feature.auth

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.model.response.ResetResult
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.validator.InvalidEnumValueException
import io.ktor.http.*
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
            call.respond(HttpStatusCode.Created, authService.register(requestBody))
        }

        /**
         * @tag Auth
         * @description Request password reset OTP
         */
        post("forget-password") {
            val requestBody = call.receive<ForgetPasswordRequest>()
            authService.forgetPassword(requestBody)
            call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.OTP_SENT))
        }

        /**
         * @tag Auth
         * @description Reset password using OTP verification
         */
        post("reset-password") {
            val requestBody = call.receive<ResetRequest>()

            when (authService.resetPassword(requestBody)) {
                is ResetResult.Success -> {
                    call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                }

                is ResetResult.InvalidOrExpiredOtp -> {
                    call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.OTP_INVALID))
                }
            }
        }
    }

    /**
     * @tag Auth
     * @description Verify user account with OTP
     */
    rateLimit(RateLimitName(RateLimitNames.OTP)) {
        get("otp-verification") {
            val userId = call.requireQueryParameter("userId")
            val otp = call.requireQueryParameter("otp")
            call.respond(HttpStatusCode.OK, authService.otpVerification(userId, otp))
        }
    }

    /**
     * @tag Auth
     * @description Refresh access token using refresh token
     */
    rateLimit(RateLimitName(RateLimitNames.REFRESH_TOKEN)) {
        post("refresh-token") {
            val requestBody = call.receive<RefreshTokenRequest>()
            val tokenPair = authService.refreshAccessToken(requestBody)
            call.respond(HttpStatusCode.OK, tokenPair)
        }
    }

    requireRole {
        /**
         * @tag Auth
         * @description Logout authenticated user
         */
        post("logout") {
            val userId = call.currentUserId
            val requestBody = call.receive<LogoutRequest>()

            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)
                authService.blacklistToken(token)
            }

            authService.logout(userId, requestBody.refreshToken)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
        }

        /**
         * @tag Auth
         * @description Change password for authenticated user
         */
        put("change-password") {
            val oldPassword = call.requireQueryParameter("oldPassword")
            val newPassword = call.requireQueryParameter("newPassword")
            val currentUserId = call.currentUserId
            authService.changePassword(currentUserId, ChangePassword(oldPassword, newPassword)).let {
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
        val userId = call.requirePathParameter("userId")
        val userTypeParam = call.requireQueryParameter("userType")

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

        val currentUserId = call.currentUserId
        if (authService.changeUserType(currentUserId, userId, newType)) {
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
        val userId = call.requirePathParameter("userId")
        val currentUserId = call.currentUserId
        if (authService.deactivateUser(currentUserId, userId)) {
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
        val userId = call.requirePathParameter("userId")
        val currentUserId = call.currentUserId
        if (authService.activateUser(currentUserId, userId)) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "User activated successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to activate user"))
        }
    }
}
