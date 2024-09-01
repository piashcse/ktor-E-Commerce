package com.piashcse.utils.extension

import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.utils.CommonException
import com.piashcse.utils.Response
import io.github.smiley4.ktorswaggerui.dsl.routes.OpenApiRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

fun String.isNotExistException(): CommonException {
    throw CommonException("$this is not Exist")
}

fun String.alreadyExistException(secondaryInfo: String = ""): CommonException {
    if (secondaryInfo.isEmpty())
        throw CommonException("$this is already Exist")
    else
        throw CommonException("$this $secondaryInfo is already Exist")
}

fun OpenApiRoute.apiResponse() {
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

suspend fun <T> query(block: () -> T): T = withContext(Dispatchers.IO) {
    transaction {
        block()
    }
}

fun PipelineContext<Unit, ApplicationCall>.getCurrentUser(): JwtTokenBody {
    return call.principal<JwtTokenBody>()!!
}