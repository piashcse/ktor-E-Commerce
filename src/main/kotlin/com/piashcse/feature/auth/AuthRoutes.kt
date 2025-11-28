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
         * @body [LoginRequest]
         * @response 200 [Response]
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
         * @body [RegisterRequest]
         * @response 200 [Response]
         */
        post("register") {
            val requestBody = call.receive<RegisterRequest>()
            call.respond(ApiResponse.success(authController.register(requestBody), HttpStatusCode.OK))
        }

        /**
         * @tag Auth
         * @query userId (required)
         * @query otp (required)
         * @response 200 [Response]
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
         * @query email (required)
         * @query userType (required)
         * @response 200 [Response]
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
         * @query email (required)
         * @query otp (required)
         * @query newPassword (required)
         * @query userType (required)
         * @response 200 [Response]
         */
        get("reset-password") {
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

        authenticate(
            RoleManagement.SUPER_ADMIN.role,
            RoleManagement.ADMIN.role,
            RoleManagement.SELLER.role,
            RoleManagement.CUSTOMER.role
        ) {
            /**
             * @tag Auth
             * @query oldPassword (required)
             * @query newPassword (required)
             * @response 200 [Response]
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

        // Admin and Super Admin routes for user management
        authenticate(RoleManagement.SUPER_ADMIN.role, RoleManagement.ADMIN.role) {
            /**
             * @tag Auth
             * @path userId (required)
             * @query userType (required)
             * @response 200 [Response]
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
             * @path userId (required)
             * @response 200 [Response]
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
             * @path userId (required)
             * @response 200 [Response]
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