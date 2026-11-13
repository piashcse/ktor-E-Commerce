@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

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
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.otpRateLimit
import com.piashcse.plugin.refreshTokenRateLimit
import com.piashcse.plugin.requireRole
import com.piashcse.plugin.standardErrors
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.bearerToken
import com.piashcse.utils.extension.clientIp
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.MissingParameterException
import com.piashcse.utils.validator.RateLimitExceededException
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class OtpVerificationRequest(val userId: String, val otp: String)

@Serializable
data class SocialLoginRequest(val idToken: String, val mfaCode: String? = null)

/**
 * Authentication and registration routes.
 */
fun Route.authRoutes() {
    val userAuthService: UserAuthenticationService by inject()
    val authRepo: AuthRepository by inject()
    val socialAuthService: SocialAuthService by inject()
    val passkeyService: PasskeyService by inject()
    // Rate-limited endpoints (brute-force protection)
    authRateLimit {
        /**
         * Authenticate user with email, password and user type
         */
        post("login") {
            call.respondOk(userAuthService.login(call.receive<LoginRequest>(), call.clientIp))
        }.describe {
            summary = "Login"
            description = "Returns an access token and rotating refresh token"
            requestBody { jsonBody<LoginRequest>() }
            responses {
                ok<TokenPair>("Authenticated")
                standardErrors()
            }
        }

        /**
         * Social login with a Google ID token
         */
        post("social/google") {
            val body = call.receive<SocialLoginRequest>()
            call.respondOk(socialAuthService.googleLogin(body.idToken, body.mfaCode))
        }.describe {
            summary = "Google social login"
            description = "Verifies a Google ID token and returns app tokens"
            requestBody { jsonBody<SocialLoginRequest>() }
        }

        /**
         * Passkey login options
         */
        post("passkeys/login/options") {
            call.respondOk(passkeyService.loginOptions(call.receive<PasskeyLoginOptionsRequest>()))
        }.describe {
            summary = "Start passkey login"
            requestBody { jsonBody<PasskeyLoginOptionsRequest>() }
        }

        /**
         * Passkey login assertion
         */
        post("passkeys/login") {
            call.respondOk(passkeyService.login(call.receive<PasskeyLoginRequest>()))
        }.describe {
            summary = "Finish passkey login"
            requestBody { jsonBody<PasskeyLoginRequest>() }
        }

        /**
         * Register a new user account
         */
        post("register") {
            call.respondCreated(userAuthService.register(call.receive<RegisterRequest>()))
        }.describe {
            summary = "Register"
            description = "Creates/refreshes an unverified account and sends an OTP"
            requestBody { jsonBody<RegisterRequest>() }
            responses {
                created<RegistrationResult>("Registration result")
                standardErrors()
            }
        }

        /**
         * Request password reset OTP
         */
        post("forgot-password") {
            userAuthService.forgotPassword(call.receive<ForgotPasswordRequest>())
            call.respondOk(MessageResponse(Message.Auth.OTP_SENT))
        }.describe {
            summary = "Request password reset OTP"
            requestBody { jsonBody<ForgotPasswordRequest>() }
            responses {
                ok<MessageResponse>("OTP sent")
                standardErrors()
            }
        }

        /**
         * Reset password using OTP verification
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
        }.describe {
            summary = "Reset password with OTP"
            requestBody { jsonBody<ResetRequest>() }
            responses {
                ok<MessageResponse>("Password reset")
                standardErrors()
            }
        }
    }

    /**
     * Verify user account with OTP
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
                    ?: throw MissingParameterException("userId")
            val otp =
                call.request.queryParameters["otp"] ?: body?.otp
                    ?: throw MissingParameterException("otp")
            if (!userAuthService.otpVerification(userId, otp)) {
                throw ValidationException(Message.Auth.OTP_INVALID)
            }
            call.respondOk(MessageResponse("OTP verified successfully"))
        }.describe {
            summary = "Verify account OTP"
            requestBody { jsonBody<OtpVerificationRequest>() }
            responses {
                ok<MessageResponse>("OTP verified")
                standardErrors()
            }
        }
    }

    /**
     * Refresh access token using refresh token
     */
    refreshTokenRateLimit {
        post("refresh-token") {
            call.respondOk(authRepo.refreshAccessToken(call.receive<RefreshTokenRequest>()))
        }.describe {
            summary = "Refresh access token"
            description = "Refresh tokens rotate and reuse revokes the session family"
            requestBody { jsonBody<RefreshTokenRequest>() }
            responses {
                ok<TokenPair>("New token pair")
                standardErrors()
            }
        }
    }

    requireRole {
        /**
         * Logout authenticated user
         */
        post("logout") {
            call.bearerToken()?.let { authRepo.blacklistToken(it) }

            // Body is optional — access token is already blacklisted above even if refresh token missing.
            val refreshToken = runCatching { call.receive<LogoutRequest>().refreshToken }.getOrNull()
            authRepo.logout(call.currentUserId, refreshToken)
            call.respondOk(MessageResponse("Logged out successfully"))
        }.describe { summary = "Logout authenticated user" }

        /**
         * Change password for authenticated user
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
                call.bearerToken()?.let { authRepo.blacklistToken(it) }
                call.respondOk(MessageResponse(Message.Auth.PASSWORD_CHANGE_SUCCESS))
            } else {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse(Message.Auth.INVALID_CREDENTIALS))
            }
        }.describe { summary = "Change password for authenticated user" }

        get("passkeys") {
            call.respondOk(passkeyService.list(call.currentUserId))
        }.describe { summary = "List my passkeys" }

        post("passkeys/register/options") {
            call.respondOk(passkeyService.registerOptions(call.currentUserId))
        }.describe { summary = "Start passkey registration" }

        post("passkeys/register") {
            call.respondOk(passkeyService.register(call.currentUserId, call.receive<PasskeyRegisterRequest>()))
        }.describe {
            summary = "Finish passkey registration"
            requestBody { jsonBody<PasskeyRegisterRequest>() }
        }

        delete("passkeys/{id}") {
            val id = call.requirePathParameter("id")
            if (!passkeyService.delete(call.currentUserId, id, call.receive<PasskeyDeleteRequest>())) {
                throw com.piashcse.utils.validator.NotFoundException("Passkey not found")
            }
            call.respondOk(MessageResponse("Passkey deleted"))
        }.describe {
            summary = "Delete a passkey"
            requestBody { jsonBody<PasskeyDeleteRequest>() }
        }

        mfaRoutes()
    }
}

/**
 * TOTP multi-factor authentication routes for the authenticated user.
 */
private fun Route.mfaRoutes() {
    val mfaService: MfaService by inject()
    val authRepo: AuthRepository by inject()
    /**
     * Start MFA setup (authenticated + password re-authentication)
     */
    post("mfa/setup") {
        val body = call.receive<MfaSetupRequest>()
        if (!authRepo.verifyUserPassword(call.currentUserId, body.password)) {
            throw ValidationException("Invalid password")
        }
        call.respondOk(mfaService.setup(call.currentUserId))
    }.describe {
        summary = "Start MFA setup"
        description = "Password re-authentication is required; returns a TOTP secret and otpauth URL"
        requestBody { jsonBody<MfaSetupRequest>() }
        responses { ok<MfaSetupResponse>("MFA setup") }
    }

    /**
     * Enable MFA after verifying a TOTP code. Password re-authentication prevents a
     * stolen access token from enrolling an attacker-controlled authenticator.
     */
    post("mfa/enable") {
        val body = call.receive<MfaCodeRequest>()
        val password = body.password?.takeIf { it.isNotBlank() } ?: throw ValidationException("Password is required")
        if (!authRepo.verifyUserPassword(call.currentUserId, password)) {
            throw ValidationException("Invalid password")
        }
        mfaService.enable(call.currentUserId, body.code)
        call.respondOk(MessageResponse("MFA enabled"))
    }.describe {
        summary = "Enable MFA"
        requestBody { jsonBody<MfaCodeRequest>() }
        responses {
            ok<MessageResponse>("MFA enabled")
            standardErrors()
        }
    }

    /**
     * Disable MFA after verifying a TOTP code
     */
    post("mfa/disable") {
        mfaService.disable(call.currentUserId, call.receive<MfaCodeRequest>().code)
        call.respondOk(MessageResponse("MFA disabled"))
    }.describe {
        summary = "Disable MFA"
        requestBody { jsonBody<MfaCodeRequest>() }
        responses {
            ok<MessageResponse>("MFA disabled")
            standardErrors()
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
         * Admin - Change user type
         *
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
        }.describe { summary = "Admin - Change user type" }

        /**
         * Admin - Deactivate a user account
         *
         */
        put("/{userId}/deactivate") {
            val userId = call.requirePathParameter("userId")
            if (authRepo.deactivateUser(call.currentUserId, userId)) {
                call.respondOk(MessageResponse("User deactivated successfully"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed to deactivate user"))
            }
        }.describe { summary = "Admin - Deactivate a user account" }

        /**
         * Admin - activate a user account
         *
         */
        put("/{userId}/activate") {
            val userId = call.requirePathParameter("userId")
            if (authRepo.activateUser(call.currentUserId, userId)) {
                call.respondOk(MessageResponse(Message.Auth.ACCOUNT_ACTIVATED))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed to activate user"))
            }
        }.describe { summary = "Admin - activate a user account" }
    }
}
