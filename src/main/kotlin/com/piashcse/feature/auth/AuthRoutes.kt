@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.model.response.RegistrationResult
import com.piashcse.model.response.ResetResult
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.authRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.docs
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.otpRateLimit
import com.piashcse.plugin.refreshTokenRateLimit
import com.piashcse.plugin.requireRole
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.clientIp
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.RateLimitExceededException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class OtpVerificationRequest(val userId: String, val otp: String)

/**
 * Authentication and registration routes.
 */
fun Route.authRoutes() {
    val userAuthService: UserAuthenticationService by inject()
    val authRepo: AuthRepository by inject()
    // Rate-limited endpoints (brute-force protection)
    authRateLimit {
        /**
         * Authenticate user with email, password and user type
         *
         * @tag Auth
         * @description Authenticate user with email, password and user type
         */
        post("login") {
            call.respondOk(userAuthService.login(call.receive<LoginRequest>(), call.clientIp))
        }.docs("Login", "Returns an access token and rotating refresh token") {
            requestBody { jsonBody<LoginRequest>() }
            responses { ok<TokenPair>("Authenticated") }
        }

        /**
         * Register a new user account
         *
         * @tag Auth
         * @description Register a new user account
         */
        post("register") {
            call.respondCreated(userAuthService.register(call.receive<RegisterRequest>()))
        }.docs("Register", "Creates/refreshes an unverified account and sends an OTP") {
            requestBody { jsonBody<RegisterRequest>() }
            responses { created<RegistrationResult>("Registration result") }
        }

        /**
         * Request password reset OTP
         *
         * @tag Auth
         * @description Request password reset OTP
         */
        post("forgot-password") {
            userAuthService.forgotPassword(call.receive<ForgotPasswordRequest>())
            call.respondOk(MessageResponse(Message.Auth.OTP_SENT))
        }.docs("Request password reset OTP") {
            requestBody { jsonBody<ForgotPasswordRequest>() }
            responses { ok<MessageResponse>("OTP sent") }
        }

        /**
         * Reset password using OTP verification
         *
         * @tag Auth
         * @description Reset password using OTP verification
         */
        post("reset-password") {
            when (authRepo.resetPassword(call.receive<ResetRequest>())) {
                is ResetResult.Success -> {
                    call.respond(HttpStatusCode.OK, MessageResponse(Message.Auth.PASSWORD_CHANGE_SUCCESS))
                }

                is ResetResult.InvalidOrExpiredOtp -> {
                    throw ValidationException(Message.Auth.OTP_INVALID)
                }

                is ResetResult.Locked -> {
                    throw RateLimitExceededException(
                        Message.Auth.accountLocked(AppConstants.Authentication.OTP_LOCKOUT_MINUTES),
                    )
                }
            }
        }.docs("Reset password with OTP") {
            requestBody { jsonBody<ResetRequest>() }
            responses { ok<MessageResponse>("Password reset") }
        }
    }

    /**
     * Verify user account with OTP
     *
     * @tag Auth
     * @description Verify user account with OTP
     */
    otpRateLimit {
        post("otp-verification") {
            // Prefer the documented JSON body; keep query params as a legacy fallback.
            val body =
                if (call.request.queryParameters["userId"] == null) {
                    runCatching { call.receive<OtpVerificationRequest>() }.getOrNull()
                } else {
                    null
                }
            val userId =
                call.request.queryParameters["userId"] ?: body?.userId
                    ?: throw ValidationException("userId is required")
            val otp =
                call.request.queryParameters["otp"] ?: body?.otp
                    ?: throw ValidationException("otp is required")
            if (!userAuthService.otpVerification(userId, otp)) {
                throw ValidationException(Message.Auth.OTP_INVALID)
            }
            call.respondOk(MessageResponse("OTP verified successfully"))
        }.docs("Verify account OTP") {
            requestBody { jsonBody<OtpVerificationRequest>() }
            responses { ok<MessageResponse>("OTP verified") }
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * @tag Auth
     * @description Refresh access token using refresh token
     */
    refreshTokenRateLimit {
        post("refresh-token") {
            call.respondOk(authRepo.refreshAccessToken(call.receive<RefreshTokenRequest>()))
        }.docs("Refresh access token", "Refresh tokens rotate and reuse revokes the session family") {
            requestBody { jsonBody<RefreshTokenRequest>() }
            responses { ok<TokenPair>("New token pair") }
        }
    }

    requireRole {
        /**
         * Logout authenticated user
         *
         * @tag Auth
         * @description Logout authenticated user
         */
        post("logout") {
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authRepo.blacklistToken(authHeader.substring(7))
            }

            // Body is optional — access token is already blacklisted above even if refresh token missing.
            val refreshToken = runCatching { call.receive<LogoutRequest>().refreshToken }.getOrNull()
            authRepo.logout(call.currentUserId, refreshToken)
            call.respondOk(MessageResponse("Logged out successfully"))
        }

        /**
         * Change password for authenticated user
         *
         * @tag Auth
         * @description Change password for authenticated user
         */
        put("change-password") {
            if (authRepo.changePassword(
                    call.currentUserId,
                    call.receive<ChangePasswordRequest>().let {
                        ChangePassword(it.oldPassword, it.newPassword)
                    },
                )
            ) {
                // Invalidate the access token used for this request as well.
                call.request.headers[HttpHeaders.Authorization]
                    ?.takeIf { it.startsWith("Bearer ") }
                    ?.substring(7)
                    ?.let { authRepo.blacklistToken(it) }
                call.respondOk(MessageResponse(Message.Auth.PASSWORD_CHANGE_SUCCESS))
            } else {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse(Message.Auth.INVALID_CREDENTIALS))
            }
        }
    }
}

/**
 * Administrative authentication/user management routes.
 */
fun Route.authAdminRoutes() {
    val authRepo: AuthRepository by inject()
    adminWriteRateLimit {
        /**
         * Admin: Change user type
         *
         * @tag Auth
         * @description Admin: Change user type
         */
        put("/{userId}/change-user-type") {
            val userId = call.requirePathParameter("userId")

            if (authRepo.changeUserType(
                    call.currentUserId,
                    userId,
                    call.requireQueryParameter("userType").parseEnum<UserType>("userType"),
                )
            ) {
                call.respondOk(MessageResponse("User type updated successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed to update user type"))
            }
        }

        /**
         * Admin: Deactivate a user account
         *
         * @tag Auth
         * @description Admin: Deactivate a user account
         */
        put("/{userId}/deactivate") {
            val userId = call.requirePathParameter("userId")
            if (authRepo.deactivateUser(call.currentUserId, userId)) {
                call.respondOk(MessageResponse("User deactivated successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed to deactivate user"))
            }
        }

        /**
         * Admin: activate a user account
         *
         * @tag Auth
         * @description Admin: activate a user account
         */
        put("/{userId}/activate") {
            val userId = call.requirePathParameter("userId")
            if (authRepo.activateUser(call.currentUserId, userId)) {
                call.respondOk(MessageResponse(Message.Auth.ACCOUNT_ACTIVATED))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed to activate user"))
            }
        }
    }
}
