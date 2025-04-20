package com.piashcse.utils.extension

import com.piashcse.database.models.user.body.JwtTokenRequest

import com.piashcse.utils.ApiResponse
import com.piashcse.utils.CommonException
import com.piashcse.utils.Response
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction

fun String.notFoundException(): CommonException {
    return CommonException("$this is not Exist")
}

fun String.alreadyExistException(secondaryInfo: String = ""): CommonException {
    return if (secondaryInfo.isEmpty()) CommonException("$this is already Exist")
    else CommonException("$this $secondaryInfo is already Exist")
}

fun RouteConfig.apiResponse() {
   return response {
        // document the "200 OK" response
        code(HttpStatusCode.OK) {
            description = "Successful"
            // specify the schema of the response body and some additional information
            body<Response> {
                description = "Successful"
            }
        }
        // document the "422 Unprocessable Entity" response
        code(HttpStatusCode.InternalServerError) {
            description = "Internal Server Error"
        }
    }
}

suspend fun <T> query(block: () -> T): T = withContext(Dispatchers.IO) {
    transaction {
        block()
    }
}
fun ApplicationCall.currentUser(): JwtTokenRequest {
    return this.principal<JwtTokenRequest>() ?: throw IllegalStateException("No authenticated user found")
}

suspend fun ApplicationCall.requiredParameters(vararg requiredParams: String): List<String>? {
    val missingParams = requiredParams.filterNot { this.parameters.contains(it) }
    if (missingParams.isNotEmpty()) {
        this.respond(ApiResponse.success("Missing parameters: $missingParams", HttpStatusCode.BadRequest))
        return null
    }
    return requiredParams.map { this.parameters[it]!! }
}