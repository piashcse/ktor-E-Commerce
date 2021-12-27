package com.example.plugins

import com.example.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

fun Application.installExceptionFeature() {
    install(StatusPages) {
        exception<Throwable> { call, error ->
            call.respond(
                HttpStatusCode.InternalServerError, JsonResponse.failure(
                    "${ErrorMessage.INTERNAL_SERVER_ERROR} : ${error.message}", HttpStatusCode.InternalServerError
                )
            )
        }

        status(HttpStatusCode.Unauthorized) { call, statusCode ->
            call.respond(HttpStatusCode.Unauthorized, JsonResponse.failure(ErrorMessage.UNAUTHORIZED, statusCode))
        }
        status(HttpStatusCode.BadRequest) { call, statusCode ->
            call.respond(JsonResponse.failure(ErrorMessage.BAD_REQUEST, statusCode))
        }
        status(HttpStatusCode.InternalServerError) { call, statusCode ->
            call.respond(
                HttpStatusCode.BadRequest, JsonResponse.failure(ErrorMessage.INTERNAL_SERVER_ERROR, statusCode)
            )
        }
        exception<TypeCastException> { call, _ ->
            call.respond(
                JsonResponse.failure(ErrorMessage.TYPE_CAST_EXCEPTION, HttpStatusCode.BadRequest)
            )
        }
        exception<NullPointerException> { call, exception ->
            call.respond(
                JsonResponse.failure(
                    "${ErrorMessage.NULL_POINTER_ERROR} ${exception.message}", HttpStatusCode.BadRequest
                )
            )
        }

        exception<UserNotExistException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest, JsonResponse.failure(ErrorMessage.USER_NOT_EXIT, HttpStatusCode.BadRequest)
            )
        }

        exception<UserTypeException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                JsonResponse.failure(ErrorMessage.USER_TYPE_IS_NOT_VALID, HttpStatusCode.BadRequest)
            )
        }
        exception<EmailNotExist> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest, JsonResponse.failure(ErrorMessage.EMAIL_NOT_EXIST, HttpStatusCode.BadRequest)
            )
        }
        exception<NoSuchElementException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest, JsonResponse.failure(ErrorMessage.USER_NOT_EXIT, HttpStatusCode.BadRequest)
            )
        }
        exception<PasswordNotMatch> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                JsonResponse.failure(ErrorMessage.PASSWORD_IS_WRONG, HttpStatusCode.BadRequest)
            )
        }
        exception<CommonException> { call, exception ->
            call.respond(HttpStatusCode.BadRequest, JsonResponse.failure(exception.message, HttpStatusCode.BadRequest))
        }
    }
}

object ErrorMessage {
    const val UNAUTHORIZED = "Unauthorized api call"
    const val INTERNAL_SERVER_ERROR = "Internal server error"
    const val BAD_REQUEST = "Parameter mismatch"
    const val USER_NOT_EXIT = "User not exist"
    const val IMAGE_UPLOAD_FAILED = "Image upload is failed"
    const val USER_TYPE_IS_NOT_VALID = "UserType is not valid"
    const val EMAIL_NOT_EXIST = "User not exist"
    const val PASSWORD_IS_WRONG = "Password is wrong"
    const val TYPE_CAST_EXCEPTION = "Type cast exception"
    const val NULL_POINTER_ERROR = "Null pointer error : "

    object MissingParameter {
        const val PROFILE_ID = "profileId"
        const val USER_ID = "userId"
    }
}
