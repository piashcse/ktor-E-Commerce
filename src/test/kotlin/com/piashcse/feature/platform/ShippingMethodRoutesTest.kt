package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.feature.shipping_method.shippingMethodAdminRoutes
import com.piashcse.model.response.ShippingMethodResponse
import com.piashcse.plugin.adminAuth
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

class ShippingMethodRoutesTest {
    private val repo: ShippingMethodRepository = mockk()

    private fun sample() =
        ShippingMethodResponse(
            id = "sm-1",
            name = "Express",
            type = "EXPRESS",
            price = "9.99",
            deliveryTime = "2-3 days",
        )

    private fun validBody() = """{"name":"Express","type":"EXPRESS","price":"9.99","deliveryTime":"2-3 days"}"""

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<ShippingMethodRepository> { repo } })
            routing { route("/api/v1/admin/shipping-methods") { adminAuth { shippingMethodAdminRoutes() } } }
        }
    }

    @Test
    fun `create happy path returns 201`() =
        testApplication {
            coEvery { repo.createShippingMethod(any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/admin/shipping-methods") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "Express")
        }

    @Test
    fun `create without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/shipping-methods") {
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `create invalid body returns 400`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/shipping-methods") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{}""")
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }

    @Test
    fun `update happy path returns 200`() =
        testApplication {
            coEvery { repo.updateShippingMethod("sm-1", any()) } returns sample()
            setup()
            val res =
                client.put("/api/v1/admin/shipping-methods/sm-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Express")
        }

    @Test
    fun `update without token returns 401`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/admin/shipping-methods/sm-1") {
                    contentType(ContentType.Application.Json)
                    setBody(validBody())
                }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `delete happy path returns 200`() =
        testApplication {
            coEvery { repo.deleteShippingMethod("sm-1") } returns true
            setup()
            val res =
                client.delete("/api/v1/admin/shipping-methods/sm-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `delete without token returns 401`() =
        testApplication {
            setup()
            val res = client.delete("/api/v1/admin/shipping-methods/sm-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
