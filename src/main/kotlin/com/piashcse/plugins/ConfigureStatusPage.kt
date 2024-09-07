package com.piashcse.plugins

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
            call.respond(
                HttpStatusCode.InternalServerError, ApiResponse.failure(
                    "Internal server error : ${error.message}", HttpStatusCode.InternalServerError
                )
            )
        }
        exception<ConstraintViolationException> { call, error ->
            val errorMessage = error.constraintViolations.mapToMessage(baseName = "messages", locale = Locale.ENGLISH)
                .map { "${it.property}: ${it.message}" }
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure(
                    errorMessage, HttpStatusCode.BadRequest
                )
            )
        }
        exception<MissingRequestParameterException> { call, error ->
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure(
                    "${error.message}", HttpStatusCode.BadRequest
                )
            )
        }
        status(HttpStatusCode.Unauthorized) { call, statusCode ->
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.failure("Unauthorized api call", statusCode))
        }
        status(HttpStatusCode.BadRequest) { call, statusCode ->
            call.respond(HttpStatusCode.BadRequest, ApiResponse.failure("Parameter missing", statusCode))
        }
        status(HttpStatusCode.InternalServerError) { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure("Internal server error", HttpStatusCode.BadRequest)
            )
        }
        exception<TypeCastException> { call, _ ->
            call.respond(
                ApiResponse.failure("Type cast exception", HttpStatusCode.BadRequest)
            )
        }
        exception<NullPointerException> { call, exception ->
            call.respond(
                ApiResponse.failure(
                    "Null pointer error : ${exception.message}", HttpStatusCode.BadRequest
                )
            )
        }
        exception<UserNotExistException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure("User not exist", HttpStatusCode.BadRequest)
            )
        }
        exception<UserTypeException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure("UserType is not valid", HttpStatusCode.BadRequest)
            )
        }
        exception<EmailNotExist> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure("User not exist", HttpStatusCode.BadRequest)
            )
        }
        exception<PasswordNotMatch> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure("Password is wrong", HttpStatusCode.BadRequest)
            )
        }
        exception<CommonException> { call, exception ->
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure(exception.message, HttpStatusCode.BadRequest)
            )
        }
        status(HttpStatusCode.NotFound) { call, data ->
            call.respond(
                HttpStatusCode.NotFound, ApiResponse.failure(data.description, HttpStatusCode.NotFound)
            )
        }
    }
}