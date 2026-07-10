package com.piashcse.feature.auth

import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.model.response.ResetResult
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.requireRole
import com.piashcse.utils.extension.*
import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Authentication and registration routes.
 */
fun Route.authRoutes() {
    val userAuthService: UserAuthenticationService by inject()
    val authRepo: AuthRepository by inject()
    // Rate-limited endpoints (brute-force protection)
    rateLimit(RateLimitName(RateLimitNames.AUTH)) {
        /**
         * @tag Auth
         * @description Authenticate user with email, password and user type
         */
        post("login") {
            call.respondOk(userAuthService.login(call.receive<LoginRequest>()))
        }

        /**
         * @tag Auth
         * @description Register a new user account
         */
        post("register") {
            call.respondCreated(userAuthService.register(call.receive<RegisterRequest>()))
        }

        /**
         * @tag Auth
         * @description Request password reset OTP
         */
        post("forgot-password") {
            userAuthService.forgotPassword(call.receive<ForgotPasswordRequest>())
            call.respondOk(mapOf("message" to Message.Auth.OTP_SENT))
        }

        /**
         * @tag Auth
         * @description Reset password using OTP verification
         */
        post("reset-password") {
            when (authRepo.resetPassword(call.receive<ResetRequest>())) {
                is ResetResult.Success -> {
                    call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                }

                is ResetResult.InvalidOrExpiredOtp -> {
                    call.respondOk(mapOf("message" to Message.Auth.OTP_INVALID))
                }
            }
        }
    }

    /**
     * @tag Auth
     * @description Verify user account with OTP
     */
    rateLimit(RateLimitName(RateLimitNames.OTP)) {
        post("otp-verification") {
            val userId = call.requireQueryParameter("userId")
            val otp = call.requireQueryParameter("otp")
            call.respondOk(userAuthService.otpVerification(userId, otp))
        }
    }

    /**
     * @tag Auth
     * @description Refresh access token using refresh token
     */
    rateLimit(RateLimitName(RateLimitNames.REFRESH_TOKEN)) {
        post("refresh-token") {
            call.respondOk(authRepo.refreshAccessToken(call.receive<RefreshTokenRequest>()))
        }
    }

    requireRole {
        /**
         * @tag Auth
         * @description Logout authenticated user
         */
        post("logout") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authRepo.blacklistToken(authHeader.substring(7))
            }

            authRepo.logout(call.currentUserId, call.receive<LogoutRequest>().refreshToken)
            call.respondOk(mapOf("message" to "Logged out successfully"))
        }

        /**
         * @tag Auth
         * @description Change password for authenticated user
         */
        put("change-password") {
            if (authRepo.changePassword(call.currentUserId, call.receive<ChangePasswordRequest>().let { ChangePassword(it.oldPassword, it.newPassword) })) {
                call.respondOk(mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to Message.Auth.INVALID_CREDENTIALS))
            }
        }
    }
}

/**
 * Administrative authentication/user management routes.
 */
fun Route.authAdminRoutes() {
    val authRepo: AuthRepository by inject()
    /**
     * @tag Auth
     * @description Admin: Change user type
     */
    put("/{userId}/change-user-type") {
        val userId = call.requirePathParameter("userId")

        if (authRepo.changeUserType(call.currentUserId, userId, call.requireQueryParameter("userType").parseEnum<UserType>("userType"))) {
            call.respondOk(mapOf("message" to "User type updated successfully"))
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
        if (authRepo.deactivateUser(call.currentUserId, userId)) {
            call.respondOk(mapOf("message" to "User deactivated successfully"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to deactivate user"))
        }
    }

    put("/{userId}/activate") {
        val userId = call.requirePathParameter("userId")
        if (authRepo.activateUser(call.currentUserId, userId)) {
            call.respondOk(mapOf("message" to "Message.Auth.ACCOUNT_ACTIVATED"))
        } else {
            call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to activate user"))
        }
    }
}
