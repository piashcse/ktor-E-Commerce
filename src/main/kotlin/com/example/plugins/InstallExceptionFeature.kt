package com.example.plugins

import com.example.plugins.ErrorMessage.NULL_POINTER_ERROR
import com.example.utils.*
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import java.lang.NullPointerException
import javax.naming.AuthenticationException

fun Application.installExceptionFeature() {
    install(StatusPages) {
        exception<Throwable> {
            call.respond(
                JsonResponse.failure(
                    "${ErrorMessage.INTERNAL_SERVER_ERROR} : $it", HttpStatusCode.InternalServerError
                )
            )
        }
        exception<MissingRequestParameterException> { exception ->
            call.respond(JsonResponse.failure(exception.message, HttpStatusCode.BadRequest))
        }
        status(HttpStatusCode.Unauthorized) { statusCode ->
            call.respond(JsonResponse.failure(ErrorMessage.UNAUTHORIZED, statusCode))
        }
        status(HttpStatusCode.BadRequest) { statusCode ->
            call.respond(JsonResponse.failure(ErrorMessage.BAD_REQUEST, statusCode))
        }
        status(HttpStatusCode.InternalServerError) { statusCode ->
            call.respond(JsonResponse.failure(ErrorMessage.INTERNAL_SERVER_ERROR, statusCode))
        }

        exception<TypeCastException> { exception ->
            call.respond(
                JsonResponse.failure(ErrorMessage.TYPE_CAST_EXCEPTION, HttpStatusCode.BadRequest)
            )
        }
        exception<NullPointerException> {
            call.respond(
                JsonResponse.failure("$NULL_POINTER_ERROR ${it.message}", HttpStatusCode.BadRequest)
            )
        }

        exception<UserNotExistException> {
            call.respond(JsonResponse.failure(ErrorMessage.USER_NOT_EXIT, HttpStatusCode.BadRequest))
        }

        exception<UserTypeException> {
            call.respond(JsonResponse.failure(ErrorMessage.USER_TYPE_IS_NOT_VALID, HttpStatusCode.BadRequest))
        }
        exception<EmailNotExist> {
            call.respond(JsonResponse.failure(ErrorMessage.EMAIL_NOT_EXIST, HttpStatusCode.BadRequest))
        }
        exception<NoSuchElementException> {
            call.respond(JsonResponse.failure(ErrorMessage.USER_NOT_EXIT, HttpStatusCode.BadRequest))
        }
        exception<AuthenticationException> {
            call.respond(JsonResponse.failure(it.message, HttpStatusCode.Unauthorized))
        }
        exception<PasswordNotMatch> {
            call.respond(JsonResponse.failure(ErrorMessage.PASSWORD_IS_WRONG, HttpStatusCode.BadRequest))
        }
        exception<ProductCategoryExist> {
            call.respond(JsonResponse.failure(ErrorMessage.PRODUCT_CATEGORY_ALREADY_EXIST, HttpStatusCode.BadRequest))
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
    const val PRODUCT_CATEGORY_ALREADY_EXIST = "Product category already exist"
    const val NULL_POINTER_ERROR = "Null pointer error : "

    object MissingParameter {
        const val PROFILE_ID = "profileId"
        const val USER_ID = "userId"
    }
}
