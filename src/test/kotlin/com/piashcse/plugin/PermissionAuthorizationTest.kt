package com.piashcse.plugin

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.Permission
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

/** Exercises the production permission gate rather than a per-feature mock. */
class PermissionAuthorizationTest {
    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { })
            routing {
                route("/secure") {
                    requirePermission(Permission.USER_MANAGE) {
                        get { call.respond(HttpStatusCode.OK, "allowed") }
                    }
                }
                route("/customer-only") {
                    customerOnlyAuth {
                        get { call.respond(HttpStatusCode.OK, "customer") }
                    }
                }
            }
        }
    }

    @Test
    fun `admin with the required permission is allowed`() =
        testApplication {
            setup()
            val res =
                client.get("/secure") {
                    header(
                        HttpHeaders.Authorization,
                        authHeader(userType = "ADMIN", permissions = setOf("user:manage")),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("allowed", res.bodyAsText())
        }

    @Test
    fun `admin without the required permission is forbidden`() =
        testApplication {
            setup()
            val res =
                client.get("/secure") {
                    header(
                        HttpHeaders.Authorization,
                        authHeader(userType = "ADMIN", permissions = emptySet()),
                    )
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `api key scope is accepted`() =
        testApplication {
            setup()
            val res =
                client.get("/secure") {
                    header(
                        HttpHeaders.Authorization,
                        authHeader(
                            userType = "CUSTOMER",
                            isApiKey = true,
                            scopes = setOf("user:manage"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `api key without the scope is forbidden`() =
        testApplication {
            setup()
            val res =
                client.get("/secure") {
                    header(
                        HttpHeaders.Authorization,
                        authHeader(
                            userType = "CUSTOMER",
                            isApiKey = true,
                            scopes = setOf("order:manage"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            val res = client.get("/secure")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `seller api key cannot pass the exact customer gate`() =
        testApplication {
            setup()
            val res =
                client.get("/customer-only") {
                    header(
                        HttpHeaders.Authorization,
                        authHeader(
                            userType = "SELLER",
                            isApiKey = true,
                            scopes = setOf("api_key:manage"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `customer api key passes the exact customer gate`() =
        testApplication {
            setup()
            val res =
                client.get("/customer-only") {
                    header(
                        HttpHeaders.Authorization,
                        authHeader(
                            userType = "CUSTOMER",
                            isApiKey = true,
                            scopes = setOf("api_key:manage"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }
}
