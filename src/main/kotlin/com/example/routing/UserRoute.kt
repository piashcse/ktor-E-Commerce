package com.example.routing

import com.example.controller.UserController
import com.example.entities.ChangePassword
import com.example.entities.UserProfile
import com.example.entities.UsersEntity
import com.example.models.*
import com.example.utils.*
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.SerialName
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.SimpleEmail
import java.io.File
import java.util.*
import javax.naming.AuthenticationException


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
                        val loginResponse =
                           LoginResponse(it, JwtConfig.makeToken(JwtTokenBody(db.id , db.email, db.userType.user_type_id)))
                        call.respond(JsonResponse.success(loginResponse, HttpStatusCode.OK))
                } ?: run {
                    call.respond(JsonResponse.failure("Email or password is wrong", HttpStatusCode.BadRequest))
                    //throw UserNotExistException()
                }
            } catch (e: Throwable) {
                throw e
            }
        }

        authenticate(AppConstants.RoleManagement.ADMIN, AppConstants.RoleManagement.MERCHANT){
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
                            "${AppConstants.SuccessMessage.VerificationCode.VERIFICATION_CODE_SEND_TO} ${forgetPasswordBody.email}",
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
      //  authenticate("auth-oauth-google") {
            post("/google-login") {
                val accessToken = call.receive<GoogleLogin>()
                val idToken:GoogleIdToken = authenticateByGoogle(accessToken.accessToken,"165959276467-4q6oqs1dt8dikeloe52g7283l42gcome.apps.googleusercontent.com")
                println(idToken.payload.email)
            }
     //   }
    }
}
private fun authenticateByGoogle(idTokenString: String, clientId: String): GoogleIdToken {
    val transport = NetHttpTransport()
    val jsonFactory = GsonFactory()
    val verifier: GoogleIdTokenVerifier = GoogleIdTokenVerifier
        .Builder(transport, jsonFactory)
        .setAudience(Collections.singletonList(clientId))
        .setIssuer("https://accounts.google.com")
        .build()

    // 確認結果がnullの場合はAuthenticationExceptionをthrowしている
    print("token : $idTokenString")
    return verifier.verify(idTokenString) ?: throw AuthenticationException()
}

data class UserInfo(
    val id: String,
    val name: String,
    @SerialName("given_name") val givenName: String,
    @SerialName("family_name") val familyName: String,
    val picture: String,
    val locale: String
)
