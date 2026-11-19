package com.piashcse.plugin

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Security headers and CSP must be applied to every non-Swagger response. */
class SecurityHeadersTest {
    @Test
    fun `security headers are present on normal responses`() =
        testApplication {
            application {
                configureBasic()
                routing {
                    get("/ping") { call.respondText("pong") }
                }
            }
            val res = client.get("/ping")
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("nosniff", res.headers["X-Content-Type-Options"])
            assertEquals("DENY", res.headers["X-Frame-Options"])
            assertEquals("no-referrer", res.headers["Referrer-Policy"])
            val csp = res.headers["Content-Security-Policy"].orEmpty()
            assertTrue(csp.contains("default-src 'self'"), "unexpected CSP: $csp")
            assertTrue(!csp.contains("unpkg.com"), "normal routes must not allow the Swagger CDN")
        }
}
