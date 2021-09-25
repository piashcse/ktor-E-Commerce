package com.example.installfeature

import com.example.utils.AppConstants
import com.example.utils.UserNotExistException
import helpers.JsonResponse
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import java.lang.NullPointerException

fun Application.installExceptionFeature(){
    install(StatusPages) {
        exception<MissingRequestParameterException> { exception ->
            call.respond(JsonResponse.failure(exception.message,  HttpStatusCode.BadRequest))
        }
        status(HttpStatusCode.Unauthorized) { statusCode ->
            call.respond(JsonResponse.failure(AppConstants.ErrorMessage.UNAUTHORIZED, statusCode))
        }
        status(HttpStatusCode.BadRequest) { statusCode ->
            call.respond(JsonResponse.failure(AppConstants.ErrorMessage.BAD_REQUEST, statusCode))
        }
        status(HttpStatusCode.InternalServerError) { statusCode ->
            call.respond(JsonResponse.failure(AppConstants.ErrorMessage.INTERNAL_SERVER_ERROR, statusCode))
        }

        exception<TypeCastException> { exception ->
            call.respond(
                JsonResponse.failure("Missing parameter..", HttpStatusCode.BadRequest)
            )
        }
        exception<NullPointerException> {
            call.respond(
                JsonResponse.failure("Missing parameter cause null", HttpStatusCode.BadRequest)
            )
        }
        exception<Throwable> {
            call.respond(JsonResponse.failure("${AppConstants.ErrorMessage.INTERNAL_SERVER_ERROR} : ${it.message}", HttpStatusCode.InternalServerError))
        }

        exception<UserNotExistException> {
            call.respond(JsonResponse.failure(AppConstants.ErrorMessage.USER_NOT_EXIT, HttpStatusCode.BadRequest))
        }
    }
}
