package com.piashcse.feature.auth

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ChangePassword
import com.piashcse.model.request.*
import com.piashcse.plugin.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.extension.requiredParameters
import com.piashcse.utils.sendEmail
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
    route("/auth") {
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
            call.respond(
                ApiResponse.success(
                    authController.login(requestBody), HttpStatusCode.OK
                )
            )
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
            call.respond(ApiResponse.success(authController.register(requestBody), HttpStatusCode.OK))
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
            val (userId, otp) = call.requiredParameters("userId", "otp") ?: return@get
            call.respond(
                ApiResponse.success(
                    authController.otpVerification(userId, otp), HttpStatusCode.OK
                )
            )
        }

        /**
         * @tag Auth
         * @description Request password reset OTP
         * @operationId forgetPassword
         * @query email (required) User email
         * @query userType (required) User type
         * @response 200 OTP sent successfully
         * @response 400 Invalid email or user type
         */
        get("forget-password") {
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
         * @tag Auth
         * @description Reset password using OTP verification
         * @operationId resetPassword
         * @query email (required) User email
         * @query otp (required) OTP received
         * @query newPassword (required) New password
         * @query userType (required) User type
         * @response 200 Password reset successful
         * @response 400 Invalid OTP or email
         */
        get("reset-password") {
            val (email, otp, newPassword, userType) = call.requiredParameters(
                "email", "otp", "newPassword", "userType"
            ) ?: return@get

            authController.resetPassword(
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

        authenticate(
            RoleManagement.SUPER_ADMIN.role,
            RoleManagement.ADMIN.role,
            RoleManagement.SELLER.role,
            RoleManagement.CUSTOMER.role
        ) {
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

        authenticate(RoleManagement.SUPER_ADMIN.role, RoleManagement.ADMIN.role) {
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
                val (userId) = call.requiredParameters("userId") ?: return@put
                val userTypeParam = call.parameters["userType"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "userType parameter is required")
                    return@put
                }

                val newType = try {
                    UserType.valueOf(userTypeParam.uppercase())
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid userType")
                    return@put
                }

                val currentUser = call.principal<JwtTokenRequest>()
                if (currentUser == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@put
                }

                try {
                    val success = authController.changeUserType(
                        currentUser.userId,
                        userId,
                        newType
                    )

                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                "User type changed successfully to $newType", HttpStatusCode.OK
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse.failure(
                                "Failed to change user type", HttpStatusCode.BadRequest
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse.failure(
                            e.message ?: "Error changing user type", HttpStatusCode.BadRequest
                        )
                    )
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
                val (userId) = call.requiredParameters("userId") ?: return@put
                val currentUser = call.principal<JwtTokenRequest>()

                if (currentUser == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@put
                }

                try {
                    val success = authController.deactivateUser(
                        currentUser.userId,
                        userId
                    )

                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                "User deactivated successfully", HttpStatusCode.OK
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse.failure(
                                "Failed to deactivate user", HttpStatusCode.BadRequest
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse.failure(
                            e.message ?: "Error deactivating user", HttpStatusCode.BadRequest
                        )
                    )
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
                val (userId) = call.requiredParameters("userId") ?: return@put
                val currentUser = call.principal<JwtTokenRequest>()

                if (currentUser == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@put
                }

                try {
                    val success = authController.activateUser(
                        currentUser.userId,
                        userId
                    )

                    if (success) {
                        call.respond(
                            ApiResponse.success(
                                "User activated successfully", HttpStatusCode.OK
                            )
                        )
                    } else {
                        call.respond(
                            ApiResponse.failure(
                                "Failed to activate user", HttpStatusCode.BadRequest
                            )
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        ApiResponse.failure(
                            e.message ?: "Error activating user", HttpStatusCode.BadRequest
                        )
                    )
                }
            }
        }
    }
}