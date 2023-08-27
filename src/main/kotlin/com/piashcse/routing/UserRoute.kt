package com.piashcse.routing

import com.piashcse.controller.UserController
import com.piashcse.entities.user.ChangePassword
import com.piashcse.entities.user.UsersEntity
import com.piashcse.models.user.body.*
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.*
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.piashcse.utils.ApiResponse
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail

fun NormalOpenAPIRoute.userRoute(userController: UserController) {
    route("login").get<LoginBody, Response> { requestBody ->
        requestBody.validation()
        respond(ApiResponse.success(userController.login(requestBody), HttpStatusCode.OK))
    }
    route("registration").post<Unit, Response, RegistrationBody> { _, requestBody ->
        requestBody.validation()
        respond(ApiResponse.success(userController.registration(requestBody), HttpStatusCode.OK))

    }
    route("forget-password").get<ForgetPasswordEmail, Response> { params ->
        params.validation()
        userController.forgetPassword(params)?.let {
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
                addTo(params.email)
                send()
            }
            respond(
                ApiResponse.success(
                    "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SENT_TO} ${params.email}",
                    HttpStatusCode.OK
                )
            )
        }
    }
    route("verify-change-password").get<ConfirmPassword, Response> { params ->
        params.validation()
        UserController().changeForgetPasswordByVerificationCode(params).let {
            when (it) {
                AppConstants.DataBaseTransaction.FOUND -> {
                    respond(
                        ApiResponse.success(
                            AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                        )
                    )
                }
                AppConstants.DataBaseTransaction.NOT_FOUND -> {
                    respond(
                        ApiResponse.success(
                            AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                            HttpStatusCode.OK
                        )
                    )
                }
            }
        }
    }
    authenticateWithJwt(RoleManagement.ADMIN.role, RoleManagement.SELLER.role, RoleManagement.USER.role) {
        route("change-password").put<ChangePassword, Response, Unit, JwtTokenBody> { params, _ ->
            userController.changePassword(principal().userId, params)?.let {
                if (it is UsersEntity) respond(
                    ApiResponse.success(
                        "Password has been changed", HttpStatusCode.OK
                    )
                )
                if (it is ChangePassword) respond(
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
