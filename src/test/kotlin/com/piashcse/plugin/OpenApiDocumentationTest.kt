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
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `all feature route files use the unified describe metadata`() {
        val routeFiles =
            File("src/main/kotlin/com/piashcse/feature")
                .walkTopDown()
                .filter { it.name.endsWith("Routes.kt") }
                .toList()
        assertTrue(routeFiles.isNotEmpty())
        routeFiles.forEach { file ->
            val source = file.readText()
            val routes = Regex("(?m)^\\s*(get|post|put|delete|patch)(\\(|\\s*\\{)").findAll(source).count()
            val describes = Regex("\\.describe\\s*\\{").findAll(source).count()
            assertTrue(describes >= routes, "${file.name}: $routes routes but $describes describe blocks")
            assertFalse(source.contains(".docs("), "${file.name}: legacy .docs helper")
            assertFalse(source.contains("@tag"), "${file.name}: legacy @tag KDoc")
            assertFalse(source.contains("@description"), "${file.name}: legacy @description KDoc")
        }
    }
}
