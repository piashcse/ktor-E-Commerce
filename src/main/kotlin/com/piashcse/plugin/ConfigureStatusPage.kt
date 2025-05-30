package com.piashcse.plugin

import com.piashcse.constants.Message
import com.piashcse.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.valiktor.ConstraintViolationException
import org.valiktor.i18n.mapToMessage
import java.util.*

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            when (error) {
                is ConstraintViolationException -> {
                    val errorMessage =
                        error.constraintViolations.mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
                            .map { "${it.property}: ${it.message}" }
                    call.respond(
                        HttpStatusCode.BadRequest, ApiResponse.failure(
                            errorMessage, HttpStatusCode.BadRequest
                        )
                    )
                }

                is MissingRequestParameterException -> {
                    call.respond(
                        HttpStatusCode.BadRequest, ApiResponse.failure(
                            "${error.message}", HttpStatusCode.BadRequest
                        )
                    )
                }

                is EmailNotExist -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.failure(Message.USER_NOT_EXIST, HttpStatusCode.BadRequest)
                    )
                }

                is NullPointerException -> {
                    call.respond(
                        ApiResponse.failure(
                            "${Message.NULL_POINTER_ERROR} ${error.message}", HttpStatusCode.BadRequest
                        )
                    )
                }

                is UserNotExistException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.failure(Message.USER_NOT_EXIST, HttpStatusCode.BadRequest)
                    )
                }

                is PasswordNotMatch -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse.failure(Message.PASSWORD_IS_WRONG, HttpStatusCode.BadRequest)
                    )
                }

                is TypeCastException -> {
                    call.respond(
                        ApiResponse.failure(Message.TYPE_CAST_EXCEPTION, HttpStatusCode.BadRequest)
                    )
                }

                is CommonException -> {
                    call.respond(
                        HttpStatusCode.BadRequest, ApiResponse.failure(error.message, HttpStatusCode.BadRequest)
                    )
                }

                else -> {
                    call.respond(
                        HttpStatusCode.InternalServerError, ApiResponse.failure(
                            "${Message.INTERNAL_SERVER_ERROR} ${error.message}", HttpStatusCode.InternalServerError
                        )
                    )
                }
            }
        }
        status(HttpStatusCode.Unauthorized) { call, statusCode ->
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.failure(Message.UNAUTHORIZED_API_CALL, statusCode))
        }
    }
}