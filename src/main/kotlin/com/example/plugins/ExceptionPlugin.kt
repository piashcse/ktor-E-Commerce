package com.example.plugins

import com.example.utils.*
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
                    "${ErrorMessage.INTERNAL_SERVER_ERROR} : ${error.message}", HttpStatusCode.InternalServerError
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
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.failure(ErrorMessage.UNAUTHORIZED, statusCode))
        }
        status(HttpStatusCode.BadRequest) { call, statusCode ->
            call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(ErrorMessage.BAD_REQUEST, statusCode))
        }
        status(HttpStatusCode.InternalServerError) { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure(ErrorMessage.INTERNAL_SERVER_ERROR, HttpStatusCode.BadRequest)
            )
        }
        exception<TypeCastException> { call, _ ->
            call.respond(
                ApiResponse.failure(ErrorMessage.TYPE_CAST_EXCEPTION, HttpStatusCode.BadRequest)
            )
        }
        exception<NullPointerException> { call, exception ->
            call.respond(
                ApiResponse.failure(
                    "${ErrorMessage.NULL_POINTER_ERROR} ${exception.message}", HttpStatusCode.BadRequest
                )
            )
        }

        exception<UserNotExistException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure(ErrorMessage.USER_NOT_EXIT, HttpStatusCode.BadRequest)
            )
        }

        exception<UserTypeException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure(ErrorMessage.USER_TYPE_IS_NOT_VALID, HttpStatusCode.BadRequest)
            )
        }
        exception<EmailNotExist> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure(ErrorMessage.EMAIL_NOT_EXIST, HttpStatusCode.BadRequest)
            )
        }
        exception<PasswordNotMatch> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure(ErrorMessage.PASSWORD_IS_WRONG, HttpStatusCode.BadRequest)
            )
        }
        exception<CommonException> { call, exception ->
            call.respond(
                HttpStatusCode.BadRequest, ApiResponse.failure(exception.message, HttpStatusCode.BadRequest)
            )
        }
    }
}

object ErrorMessage {
    const val UNAUTHORIZED = "Unauthorized api call"
    const val INTERNAL_SERVER_ERROR = "Internal server error"
    const val BAD_REQUEST = "Parameter mismatch"
    const val USER_NOT_EXIT = "User not exist"
    const val USER_TYPE_IS_NOT_VALID = "UserType is not valid"
    const val EMAIL_NOT_EXIST = "User not exist"
    const val PASSWORD_IS_WRONG = "Password is wrong"
    const val TYPE_CAST_EXCEPTION = "Type cast exception"
    const val NULL_POINTER_ERROR = "Null pointer error : "
}
