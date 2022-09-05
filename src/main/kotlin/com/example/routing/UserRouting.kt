package com.example.routing

import com.example.controller.UserController
import com.example.entities.user.ChangePassword
import com.example.entities.user.UsersEntity
import com.example.models.user.body.*
import com.example.models.user.response.LoginResponse
import com.example.utils.*
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.example.utils.CustomResponse
import com.example.utils.extension.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import java.io.File
import java.util.*
import javax.naming.AuthenticationException

fun NormalOpenAPIRoute.userRoute(userController: UserController) {
    route("user/") {
        route("login").post<Unit, Response, LoginBody>(
            exampleRequest = LoginBody(
                email = "piash599@gmail.com", password = "piash007", userType = "2"
            )
        ) { _, loginBody ->
            loginBody.validation()
            userController.login(loginBody).let {
                val loginResponse =
                    LoginResponse(it, JwtConfig.makeToken(JwtTokenBody(it.id, it.email, it.userType.userTypeId)))
                respond(CustomResponse.success(loginResponse, HttpStatusCode.OK))
            }
        }

        route("registration").post<Unit, Response, RegistrationBody>() { _, registrationBody ->
            registrationBody.validation()
            respond(CustomResponse.success(userController.registration(registrationBody), HttpStatusCode.OK))

        }
        route("forget-password").post<Unit, Response, ForgetPasswordBody> { _, forgetPasswordBody ->
            forgetPasswordBody.validation()
            userController.forgetPassword(forgetPasswordBody).let {
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
                    addTo(forgetPasswordBody.email)
                    send()
                }
                respond(
                    CustomResponse.success(
                        "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SEND_TO} ${forgetPasswordBody.email}",
                        HttpStatusCode.OK
                    )
                )
            }
        }
        route("verify-password-change").post<Unit, Response, ConfirmPasswordBody> { _, confirmPasswordBody ->
            confirmPasswordBody.validation()
            UserController().confirmPassword(confirmPasswordBody).let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        respond(
                            CustomResponse.success(
                                AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                            )
                        )
                    }
                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        respond(
                            CustomResponse.success(
                                AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                                HttpStatusCode.OK
                            )
                        )
                    }
                }
            }
        }
        authenticateWithJwt(AppConstants.RoleManagement.ADMIN, AppConstants.RoleManagement.MERCHANT) {
            route("change-password").put<UserId, Response, ChangePassword, JwtTokenBody>(
                exampleRequest = ChangePassword(
                    "12345", "54321"
                )
            ) { params, requestBody ->
                params.validation()
                userController.changePassword(params.userId, requestBody)?.let {
                    if (it is UsersEntity) respond(
                        CustomResponse.success(
                            "Password hase been changed", HttpStatusCode.OK
                        )
                    )
                    if (it is ChangePassword) respond(
                        CustomResponse.failure(
                            "Old password is wrong", HttpStatusCode.OK
                        )
                    )
                } ?: run {
                    throw UserNotExistException()
                }
            }

            route("photo-upload").put<UserId, Response, MultipartImage, JwtTokenBody> { params, multipartData ->
                params.validation()
                multipartData.validation()
                val fileNameInServer =
                    "${AppConstants.Image.PROFILE_IMAGE_LOCATION}${UUID.randomUUID()}.${multipartData.file.name}"
                File(fileNameInServer).writeBytes(withContext(Dispatchers.IO) {
                    multipartData.file.readAllBytes()
                })
                userController.updateProfileImage(params.userId, multipartData.file.name)?.let {
                    respond(
                        CustomResponse.success(fileNameInServer, HttpStatusCode.OK)
                    )
                }
            }
        }
    }

/*route("user/") {
    post("registration") {
        val userBody = call.receive<RegistrationBody>()
        userBody.validation()
        val db = userController.registration(userBody)
        db.let {
            call.respond(CustomResponse.success(it, HttpStatusCode.OK))
        }
    }

    post("login") {
        val loginBody = call.receive<LoginBody>()
        loginBody.validation()
        val db = userController.login(loginBody)
        db.let {
            val loginResponse =
                LoginResponse(it, JwtConfig.makeToken(JwtTokenBody(db.id, db.email, db.userType.userTypeId)))
            call.respond(CustomResponse.success(loginResponse, HttpStatusCode.OK))
        }
    }

    authenticate(AppConstants.RoleManagement.ADMIN, AppConstants.RoleManagement.MERCHANT) {
        post("update-profile") {
            val profileId = call.request.queryParameters["userId"]
            if (profileId != null) {
                val profileBody = call.receive<UserProfile>()
                val db = userController.updateProfile(profileId, profileBody)
                db?.let {
                    call.respond(CustomResponse.success(it, HttpStatusCode.OK))
                } ?: run {
                    throw UserNotExistException()
                }
            } else {
                throw MissingRequestParameterException(ErrorMessage.MissingParameter.PROFILE_ID)
            }
        }
        post("change-password") {
            val userId = call.request.queryParameters["userId"]
            if (userId != null) {
                val changePasswordBody = call.receive<ChangePassword>()
                changePasswordBody.nullProperties() {
                    throw MissingRequestParameterException(it.toString())
                }
                val db = userController.changePassword(userId, changePasswordBody)
                db?.let {
                    if (it is UsersEntity) call.respond(
                        CustomResponse.success(
                            it.response(), HttpStatusCode.OK
                        )
                    )
                    if (it is ChangePassword) call.respond(
                        CustomResponse.failure(
                            "Old password is wrong", HttpStatusCode.OK
                        )
                    )
                } ?: run {
                    throw UserNotExistException()
                }
            } else {
                throw MissingRequestParameterException(ErrorMessage.MissingParameter.USER_ID)
            }
        }

        post("profile-photo") {
            val multipartData = call.receiveMultipart()
            val images = arrayListOf<String>()
            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        println("${part.name} : ${part.value}")
                    }
                    is PartData.FileItem -> {
                        val fileName = part.originalFileName as String
                        val fileBytes = part.streamProvider().readBytes()
                        val fileNameInServer =
                            "${AppConstants.Image.PROFILE_IMAGE_LOCATION}${UUID.randomUUID()}.${fileName.fileExtension()}"
                        File(fileNameInServer).writeBytes(fileBytes)
                        images += fileNameInServer
                    }
                    else -> {
                        call.respond(CustomResponse.failure(ErrorMessage.IMAGE_UPLOAD_FAILED, HttpStatusCode.OK))
                    }
                }
                part.dispose
            }
            call.principal<JwtTokenBody>()?.let {
                val db = userController.updateProfileImage(it.userId, images.first())
                db?.let {
                    call.respond(
                        CustomResponse.success(images.toArray(), HttpStatusCode.OK)
                    )
                }
            }
        }
    }
    post("forget-password") {
        val forgetPasswordBody = call.receive<ForgetPasswordBody>()
        forgetPasswordBody.validation()
        val db = userController.forgetPassword(forgetPasswordBody)
        db.let {
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
                addTo(forgetPasswordBody.email)
                send()
            }
            *//* HtmlEmail().apply {
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
                 }*//*
                call.respond(
                    CustomResponse.success(
                        "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SEND_TO} ${forgetPasswordBody.email}",
                        HttpStatusCode.OK
                    )
                )
            }
        }

        post("confirm-password") {
            val confirmPasswordBody = call.receive<ConfirmPasswordBody>()
            confirmPasswordBody.nullProperties {
                throw MissingRequestParameterException(it.toString())
            }
            val db = UserController().confirmPassword(confirmPasswordBody)
            db.let {
                when (it) {
                    AppConstants.DataBaseTransaction.FOUND -> {
                        call.respond(
                            CustomResponse.success(
                                AppConstants.SuccessMessage.Password.PASSWORD_CHANGE_SUCCESS, HttpStatusCode.OK
                            )
                        )
                    }
                    AppConstants.DataBaseTransaction.NOT_FOUND -> {
                        call.respond(
                            CustomResponse.success(
                                AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_IS_NOT_VALID,
                                HttpStatusCode.OK
                            )
                        )
                    }
                }
            }
        }
        //  authenticate("auth-oauth-google") {
        post("/google-login") {
            val accessToken = call.receive<GoogleLogin>()
            val idToken: GoogleIdToken = authenticateByGoogle(
                accessToken.accessToken, "165959276467-4q6oqs1dt8dikeloe52g7283l42gcome.apps.googleusercontent.com"
            )
            println(idToken.payload.email)
        }
        //   }
    }*/
}

private fun authenticateByGoogle(idTokenString: String, clientId: String): GoogleIdToken {
    val transport = NetHttpTransport()
    val jsonFactory = GsonFactory()
    val verifier: GoogleIdTokenVerifier =
        GoogleIdTokenVerifier.Builder(transport, jsonFactory).setAudience(Collections.singletonList(clientId))
            .setIssuer("https://accounts.google.com").build()

    // 確認結果がnullの場合はAuthenticationExceptionをthrowしている
    print("token : $idTokenString")
    return verifier.verify(idTokenString) ?: throw AuthenticationException()
}
