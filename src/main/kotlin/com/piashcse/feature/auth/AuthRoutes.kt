package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.model.request.*
import com.piashcse.model.response.ResetResult
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.customerAuth
import com.piashcse.utils.extension.clientIp
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
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
    // Rate-limited endpoints (brute-force protection)
    rateLimit(RateLimitName(RateLimitNames.AUTH)) {
        /**
         * @tag Auth
         * @description Authenticate user with email, password and user type
         */
        post("login") {
            call.respondOk(userAuthService.login(call.receive<LoginRequest>(), call.clientIp))
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
            when (userAuthService.resetPassword(call.receive<ResetRequest>())) {
                is ResetResult.Success -> {
                    call.respond(HttpStatusCode.OK, mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
                }
                is ResetResult.InvalidOrExpiredOtp -> {
                    call.respondOk(mapOf("message" to Message.Auth.OTP_INVALID))
                }
                is ResetResult.Locked -> {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("message" to Message.Auth.accountLocked(AppConstants.Authentication.OTP_LOCKOUT_MINUTES)),
                    )
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
            call.respondOk(userAuthService.refreshAccessToken(call.receive<RefreshTokenRequest>()))
        }
    }

    customerAuth {
        /**
         * @tag Auth
         * @description Logout authenticated user
         */
        post("logout") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                userAuthService.blacklistToken(authHeader.substring(7))
            }

            userAuthService.logout(call.currentUserId, call.receive<LogoutRequest>().refreshToken)
            call.respondOk(mapOf("message" to "Logged out successfully"))
        }

        /**
         * @tag Auth
         * @description Change password for authenticated user
         */
        rateLimit(RateLimitName(RateLimitNames.WRITE)) {
            put("change-password") {
                if (userAuthService.changePassword(call.currentUserId, call.receive<ChangePasswordRequest>().let { ChangePassword(it.oldPassword, it.newPassword) })) {
                    call.respondOk(mapOf("message" to Message.Auth.PASSWORD_CHANGE_SUCCESS))
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
fun Route.authAdminRoutes() {
    val userAuthService: UserAuthenticationService by inject()
    rateLimit(RateLimitName(RateLimitNames.ADMIN_WRITE)) {
        /**
         * @tag Auth
         * @description Admin: Change user type
         */
        put("/{userId}/change-user-type") {
            val userId = call.requirePathParameter("userId")

            if (userAuthService.changeUserType(call.currentUserId, userId, call.requireQueryParameter("userType").parseEnum<UserType>("userType"))) {
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
            if (userAuthService.deactivateUser(call.currentUserId, userId)) {
                call.respondOk(mapOf("message" to "User deactivated successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to deactivate user"))
            }
        }

        /**
         * @tag Auth
         * @description Admin: activate a user account
         */
        put("/{userId}/activate") {
            val userId = call.requirePathParameter("userId")
            if (userAuthService.activateUser(call.currentUserId, userId)) {
                call.respondOk(mapOf("message" to Message.Auth.ACCOUNT_ACTIVATED))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to activate user"))
            }
        }
    }
}
