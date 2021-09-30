package com.example.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.controller.UserController
import com.example.entities.ChangePassword
import com.example.entities.UserProfile
import com.example.entities.UsersEntity
import com.example.models.*
import com.example.utils.*
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import org.apache.commons.mail.SimpleEmail
import java.io.File

fun Route.userRoute(userController: UserController) {
    route("user/") {
        post("registration") {
            val userBody = call.receive<RegistrationBody>()
            nullProperties(userBody) {
                if (it.isNotEmpty()) {
                    throw MissingRequestParameterException(it.toString())
                }
            }
            if (!AppConstants.ALL_USERS_TYPE.contains(userBody.userType)) {
                throw UserTypeException()
            }
            try {
                val db = userController.registration(userBody)
                db?.let {
                    call.respond(JsonResponse.success(it, HttpStatusCode.OK))
                } ?: run {
                    call.respond(JsonResponse.failure("User already exists", HttpStatusCode.BadRequest))
                }
            } catch (e: Throwable) {
                throw e
            }
        }

        post("login") {
            val loginBody = call.receive<LoginBody>()
            nullProperties(loginBody) {
                if (it.isNotEmpty()) {
                    throw MissingRequestParameterException(it.toString())
                }
            }
            try {
                val db = userController.login(loginBody)
                db?.let {
                    if (BCrypt.verifyer().verify(loginBody.password.toCharArray(), it.password).verified) {
                        val loginResponse =
                            LoginResponse(db.userResponse(), JwtConfig.makeToken(JwtTokenBody(db.id.value, db.email)))
                        call.respond(JsonResponse.success(loginResponse, HttpStatusCode.OK))
                    } else {
                        call.respond(JsonResponse.failure("Email or password is wrong", HttpStatusCode.BadRequest))
                    }
                } ?: run {
                    throw UserNotExistException()
                }
            } catch (e: Throwable) {
                throw e
            }
        }

        authenticate {
            post("update-profile") {
                try {
                    val profileId = call.request.queryParameters["userId"]
                    if (profileId != null) {
                        val profileBody = call.receive<UserProfile>()
                        val db = userController.updateProfile(profileId, profileBody)
                        db?.let {
                            call.respond(JsonResponse.success(db.userProfileResponse(), HttpStatusCode.OK))
                        } ?: run {
                            throw UserNotExistException()
                        }
                    } else {
                        throw MissingRequestParameterException(AppConstants.ErrorMessage.MissingParameter.PROFILE_ID)
                    }
                } catch (e: Throwable) {
                    throw e
                }
            }
            post("change-password") {
                try {
                    val userId = call.request.queryParameters["userId"]
                    if (userId != null) {
                        val changePasswordBody = call.receive<ChangePassword>()
                        nullProperties(changePasswordBody) {
                            if (it.isNotEmpty()) {
                                throw MissingRequestParameterException(it.toString())
                            }
                        }
                        val db = userController.changePassword(userId, changePasswordBody)
                        db?.let {
                            if (it is UsersEntity)
                                call.respond(JsonResponse.success(it.userResponse(), HttpStatusCode.OK))
                            if (it is ChangePassword)
                                call.respond(JsonResponse.failure("Old password is wrong", HttpStatusCode.OK))
                        } ?: run {
                            throw UserNotExistException()
                        }
                    } else {
                        throw MissingRequestParameterException(AppConstants.ErrorMessage.MissingParameter.USER_ID)
                    }
                } catch (e: Throwable) {
                    throw e
                }
            }

            post("profile-photo") {
                var fileDescription = ""
                var fileName = ""
                val multipartData = call.receiveMultipart()
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            fileDescription = part.value
                        }
                        is PartData.FileItem -> {
                            fileName = part.originalFileName as String
                            val fileBytes = part.streamProvider().readBytes()
                            File("${AppConstants.Image.IMAGE_FOLDER_LOCATION}$fileName").writeBytes(fileBytes)
                        }
                        else -> {
                            call.respond(JsonResponse.failure(AppConstants.ErrorMessage.IMAGE_UPLOAD_FAILED, HttpStatusCode.OK))
                        }
                    }
                }
                call.respond(JsonResponse.success("$fileDescription is uploaded to 'uploads/$fileName", HttpStatusCode.OK))
            }

        }
        post("forget-password") {
            try {
                val forgetPasswordBody = call.receive<ForgetPasswordBody>()
                nullProperties(forgetPasswordBody) {
                    if (it.isNotEmpty()) {
                        throw MissingRequestParameterException(it.toString())
                    }
                }
                val db = userController.forgetPassword(forgetPasswordBody)
                db?.let {
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
                        setMsg("Your verification id is : ${it.verificationCode}")
                        addTo(forgetPasswordBody.email)
                        send()
                    }
                    /* HtmlEmail().apply {
                         hostName = AppConstants.SmtpServer.HOST_NAME
                         setSmtpPort(AppConstants.SmtpServer.PORT)
                         setAuthenticator(DefaultAuthenticator(AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR, AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR_PASSWORD))
                         isSSLOnConnect = true
                         setFrom("piash599@gmail.com")
                         subject = AppConstants.SmtpServer.EMAIL_SUBJECT
                         setHtmlMsg("<html>\n" +
                                 "<head>\n" +
                                 "<title>Page Title</title>\n" +
                                 "</head>\n" +
                                 "<body>\n" +
                                 "\n" +
                                 "<h1>This is a Heading</h1>\n" +
                                 "<p>This is a paragraph.</p>\n" +
                                 "\n" +
                                 "</body>\n" +
                                 "</html>")
                         //setHtmlMsg("Your verification id is : ${it.verificationCode}")
                         addTo(forgetPasswordBody.email)
                         send()
                     }*/
                    call.respond(
                        JsonResponse.failure(
                            "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SEND_TO}${forgetPasswordBody.email}",
                            HttpStatusCode.OK
                        )
                    )
                } ?: run {
                    throw EmailNotExist()
                }
            } catch (e: Throwable) {
                throw e
            }
        }

        post("confirm-password") {
            try {
                val confirmPasswordBody = call.receive<ConfirmPasswordBody>()
                nullProperties(confirmPasswordBody) {
                    if (it.isNotEmpty()) {
                        throw MissingRequestParameterException(it.toString())
                    }
                }
                val db = UserController().confirmPassword(confirmPasswordBody)
                db?.let {
                    when (it) {
                        AppConstants.DataBaseTransaction.FOUND -> {
                            call.respond(
                                JsonResponse.success(
                                    AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS,
                                    HttpStatusCode.OK
                                )
                            )
                        }
                        AppConstants.DataBaseTransaction.NOT_FOUND -> {
                            call.respond(
                                JsonResponse.success(
                                    AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                                    HttpStatusCode.OK
                                )
                            )
                        }
                    }
                } ?: run {
                    throw EmailNotExist()
                }
            } catch (e: Throwable) {
                throw e
            }
        }
    }
}
