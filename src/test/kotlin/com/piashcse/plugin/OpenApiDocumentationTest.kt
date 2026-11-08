package com.piashcse.plugin

import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.AppConstants
import com.piashcse.utils.common.MessageResponse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class OpenApiDocumentationTest {
    @Test
    fun `swagger exposes info, tags, bearer security and described operations`() =
        testApplication {
            application {
                installTestInfra(module { })
                routing {
                    authenticate(AppConstants.Authentication.JWT_AUTHENTICATOR) {
                        get("sample") {
                            call.respond(MessageResponse("ok"))
                        }.describe {
                            summary = "Sample operation"
                            description = "A described sample"
                            responses { ok<MessageResponse>("OK") }
                        }
                    }
                }
                configureSwagger()
            }

            val response = client.get("/swagger/documentation.yaml")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertContains(body, "openapi: 3.1.1")
            assertContains(body, "Ktor E-Commerce API")
            assertContains(body, "Sample operation")
            assertContains(body, "jwt-auth")
            assertContains(body, "bearer")
            assertContains(body, "/sample")
        }
}
