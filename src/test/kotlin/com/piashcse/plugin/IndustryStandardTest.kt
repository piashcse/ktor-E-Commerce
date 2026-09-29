package com.piashcse.plugin

import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.model.response.HealthResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class IndustryStandardTest {
    @Test
    fun `metrics endpoint exposes prometheus scrape`() =
        testApplication {
            application {
                installTestInfra(org.koin.dsl.module { })
                configureMetrics()
            }
            client.get("/metrics")
            val res = client.get("/metrics")
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.bodyAsText()
            assertContains(body, "# HELP")
            // Request timer for the first scrape proves per-route metrics work.
            assertContains(body, "http_server_requests_seconds")
        }

    @Test
    fun `health response serializes with typed schema`() {
        val json =
            Json.encodeToString(
                HealthResponse(status = "UP", version = "1.0.0", timestamp = "2026-01-01T00:00:00"),
            )
        assertContains(json, "\"status\":\"UP\"")
        assertContains(json, "\"version\":\"1.0.0\"")
    }
}
