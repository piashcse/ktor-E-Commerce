package com.piashcse.route

import com.piashcse.controller.UserController
import com.piashcse.entities.user.ChangePassword
import com.piashcse.entities.user.UsersEntity
import com.piashcse.models.user.body.*
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.*
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.put
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail

fun Route.userRouteV2(userController: UserController) {
    get("Login", {
        tags("User")
        request {
            queryParameter<String>("email")
            queryParameter<String>("password")
            queryParameter<String>("userType")
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful"
                body<Response> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    description = "Successful"
                }
            }
            HttpStatusCode.InternalServerError
        }
    }) {
        val requiredParams = listOf("email", "password", "userType")
        requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
            if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
        }
        val (email, password, userType) = requiredParams.map { call.parameters[it]!! }
        call.respond(
            ApiResponse.success(
                userController.login(LoginBody(email, password, userType)), HttpStatusCode.OK
            )
        )
    }
    post("registration", {
        tags("User")
        request {
            body<RegistrationBody>()
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful"
                body<Response> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    description = "Successful"
                }
            }
            HttpStatusCode.InternalServerError
        }
    }) {
        val requestBody = call.receive<RegistrationBody>()
        requestBody.validation()
        call.respond(ApiResponse.success(userController.registration(requestBody), HttpStatusCode.OK))
    }
    get("forget-password", {
        tags("User")
        request {
            queryParameter<String>("email")
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful"
                body<Response> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    description = "Successful"
                }
            }
            HttpStatusCode.InternalServerError
        }
    }) {
        val requiredParams = listOf("email")
        requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
            if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
        }
        val (email) = requiredParams.map { call.parameters[it]!! }
        val requestBody = ForgetPasswordEmail(email)
        userController.forgetPassword(requestBody)?.let {
            SimpleEmail().apply {
                hostName = AppConstants.SmtpServer.HOST_NAME
                setSmtpPort(AppConstants.SmtpServer.PORT)
                setAuthenticator(
                    DefaultAuthenticator(
                        AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR,
                        AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR_PASSWORD
                    )
                )
                isSSLOnConnect = true
                setFrom("piash599@gmail.com")
                subject = AppConstants.SmtpServer.EMAIL_SUBJECT
                setMsg("Your verification code is : ${it.verificationCode}")
                addTo(requestBody.email)
                send()
            }
            call.respond(

                ApiResponse.success(
                    "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SENT_TO} ${requestBody.email}",
                    HttpStatusCode.OK
                )
            )
        }
    }
    get("verify-change-password", {
        tags("User")
        request {
            queryParameter<String>("email")
            queryParameter<String>("verificationCode")
            queryParameter<String>("newPassword")
        }
        response {
            HttpStatusCode.OK to {
                description = "Successful"
                body<Response> {
                    mediaTypes = setOf(ContentType.Application.Json)
                    description = "Successful"
                }
            }
            HttpStatusCode.InternalServerError
        }
    }) {
        val requiredParams = listOf("email", "verificationCode", "newPassword")
        requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
            if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
        }
        val (email, verificationCode, newPassword) = requiredParams.map { call.parameters[it]!! }

        UserController().changeForgetPasswordByVerificationCode(ConfirmPassword(email, verificationCode, newPassword))
            .let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        call.respond(
                            ApiResponse.success(
                                AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                            )
                        )
                    }

                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        call.respond(
                            ApiResponse.success(
                                AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                                HttpStatusCode.OK
                            )
                        )
                    }
                }
            }
    }
    authenticate(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.CUSTOMER.role) {
        put("change-password", {
            tags("User")
            protected = true
            request {
                queryParameter<String>("oldPassword")
                queryParameter<String>("newPassword")
            }
            response {
                HttpStatusCode.OK to {
                    description = "Successful"
                    body<Response> {
                        mediaTypes = setOf(ContentType.Application.Json)
                        description = "Successful"
                    }
                }
                HttpStatusCode.InternalServerError
            }
        }) {
            val requiredParams = listOf("oldPassword", "newPassword")
            requiredParams.filterNot { call.request.queryParameters.contains(it) }.let {
                if (it.isNotEmpty()) call.respond(ApiResponse.success("Missing parameters: $it", HttpStatusCode.OK))
            }
            val (oldPassword, newPassword) = requiredParams.map { call.parameters[it]!! }
            val loginUser = call.principal<JwtTokenBody>()
            userController.changePassword(loginUser?.userId!!, ChangePassword(oldPassword, newPassword))?.let {
                if (it is UsersEntity) call.respond(
                    ApiResponse.success(
                        "Password has been changed", HttpStatusCode.OK
                    )
                )
                if (it is ChangePassword) call.respond(
                    ApiResponse.failure(
                        "Old password is wrong", HttpStatusCode.OK
                    )
                )
            } ?: run {
                throw UserNotExistException()
            }
        }
    }
}