package com.example.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.controller.UserController
import com.example.entities.ChangePassword
import com.example.entities.UserProfile
import com.example.entities.UsersEntity
import com.example.models.JwtTokenBody
import com.example.models.LoginBody
import com.example.models.LoginResponse
import com.example.models.RegistrationBody
import com.example.utils.JwtConfig
import com.example.utils.UserNotExistException
import com.example.utils.nullProperties
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.userRoute(userController: UserController) {
    route("user/") {
        post("registration") {
            val userBody = call.receive<RegistrationBody>()
            nullProperties(userBody) {
                if (it.isNotEmpty()) {
                    throw MissingRequestParameterException(it.toString())
                }
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
                        throw MissingRequestParameterException("profile id")
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
                        throw MissingRequestParameterException("userId id")
                    }
                } catch (e: Throwable) {
                    throw e
                }
            }
        }
    }
}