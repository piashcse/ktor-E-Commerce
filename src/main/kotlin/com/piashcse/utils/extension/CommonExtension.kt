package com.piashcse.utils.extension

import com.piashcse.utils.CommonException
import com.piashcse.utils.Response
import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*

fun String.isNotExistException(): CommonException {
    throw CommonException("$this is not Exist")
}

fun String.alreadyExistException(secondaryInfo: String = ""): CommonException {
    if (secondaryInfo.isEmpty())
        throw CommonException("$this is already Exist")
    else
        throw CommonException("$this $secondaryInfo is already Exist")
}

fun OpenApiRoute.apiResponse(){
    return response {
        HttpStatusCode.OK to {
            description = "Successful"
            body<Response> {
                mediaTypes = setOf(ContentType.Application.Json)
                description = "Successful"
            }
        }
        HttpStatusCode.InternalServerError
    }
}