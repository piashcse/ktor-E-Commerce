package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.featureflag.FeatureFlagResponse
import com.piashcse.feature.featureflag.FeatureFlags
import com.piashcse.feature.featureflag.featureFlagRoutes
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class FeatureFlagRoutesTest {
    private val featureFlags: FeatureFlags = mockk()

    private fun sample() = FeatureFlagResponse(key = "guest_checkout", enabled = true, description = null, updatedAt = null)

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { featureFlags } })
            routing { route("/api/v1/admin/feature-flags") { featureFlagRoutes() } }
        }
    }

    @Test
    fun `super admin can list flags`() =
        testApplication {
            coEvery { featureFlags.list() } returns listOf(sample())
            setup()
            val res =
                client.get("/api/v1/admin/feature-flags") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SUPER_ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "guest_checkout")
        }

    @Test
    fun `admin without permission is forbidden`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/feature-flags") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `set flag returns updated flag`() =
        testApplication {
            coEvery { featureFlags.set("guest_checkout", any()) } returns sample()
            setup()
            val res =
                client.put("/api/v1/admin/feature-flags/guest_checkout") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SUPER_ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"enabled":true}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "guest_checkout")
        }

    @Test
    fun `anonymous is unauthorized`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/feature-flags")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
