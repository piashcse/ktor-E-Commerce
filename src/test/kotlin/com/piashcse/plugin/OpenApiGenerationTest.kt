package com.piashcse.plugin

import com.piashcse.RouteTestHelper.installTestInfra
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Builds the real OpenAPI document from the production route tree and writes it
 * to `build/endpoints.yaml` as a verification artifact.
 *
 * This keeps Swagger UI and the hand-maintained endpoint documentation honest:
 * the test fails if the route surface shrinks unexpectedly.
 */
class OpenApiGenerationTest {
    @Test
    fun `every feature route is present in the generated document`() =
        testApplication {
            application {
                installTestInfra(module { })
                configureRoute()
                configureSwagger()
            }
            val response = client.get("/swagger/documentation.yaml")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()

            assertContains(body, "openapi: 3.1.1")
            assertContains(body, "/api/v1/orders")
            assertContains(body, "/api/v1/seller/orders")
            assertContains(body, "/api/v1/admin/orders")
            assertContains(body, "/api/v1/seller/kyc")
            assertContains(body, "/api/v1/admin/outbox/summary")
            assertContains(body, "/health/ready")

            val operations = Regex("(?m)^    (get|post|put|patch|delete):").findAll(body).count()
            assertTrue(operations >= 200, "expected the full API surface, found $operations operations")

            File("build/endpoints.yaml").writeText(body)
        }
}
