package com.piashcse.route

import com.piashcse.configureAuthTest
import com.piashcse.controller.ShopCategoryController
import com.piashcse.entities.shop.ShopCategory
import com.piashcse.generateJwtToken
import com.piashcse.plugins.RoleManagement
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlin.test.Test

class ShopCategoryTest {
    private fun Application.testModule(shopCategoryController: ShopCategoryController) {
        configureAuthTest()
        routing {
            shopCategoryRoute(shopCategoryController)
        }
    }

    @Test
    fun `test adding shop category`() = testApplication {
        val mockShopCategoryController = mockk<ShopCategoryController>(relaxed = true)

        // Mock the behavior of the addShopCategory function
        coEvery { mockShopCategoryController.addShopCategory(any()) } returns ShopCategory("1", "Electronics")

        // Initialize the test application with the mock controller
        application {
            testModule(mockShopCategoryController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val authHeader = request.headers[HttpHeaders.Authorization]
                    if (request.url.encodedPath == "/shop-category" && request.method == HttpMethod.Post) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.ADMIN.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }

                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":{"id":"1","name":"Electronics"}}""",
                                    HttpStatusCode.OK
                                )
                            } else {
                                respond("Unauthorized", HttpStatusCode.Unauthorized)
                            }
                        } else {
                            respond("Unauthorized", HttpStatusCode.Unauthorized)
                        }

                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken(RoleManagement.ADMIN.role) // Generate a token for ADMIN role
        // Send POST request with the authorization header
        val response: HttpResponse = client.post("/shop-category") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Electronics"}""") // Mock request body
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":{"id":"1","name":"Electronics"}}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test getting shop categories with authentication in engine`() = testApplication {
        val mockShopCategoryController = mockk<ShopCategoryController>(relaxed = true)

        // Mock the behavior of the getShopCategories function
        coEvery { mockShopCategoryController.getShopCategories(any(), any()) } returns listOf(
            ShopCategory("1", "Electronics"),
            ShopCategory("2", "Clothing")
        )

        application {
            testModule(mockShopCategoryController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val authHeader = request.headers[HttpHeaders.Authorization]
                    if (request.url.encodedPath == "/shop-category" && request.method == HttpMethod.Get &&
                        request.url.parameters["limit"] == "10" && request.url.parameters["offset"] == "0"
                    ) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.ADMIN.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }

                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":[{"id":"1","name":"Electronics"},{"id":"2","name":"Clothing"}]}""",
                                    HttpStatusCode.OK
                                )
                            } else {
                                respond("Unauthorized", HttpStatusCode.Unauthorized)
                            }
                        } else {
                            respond("Unauthorized", HttpStatusCode.Unauthorized)
                        }
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken(RoleManagement.ADMIN.role) // Generate a token for ADMIN role
        // Send GET request with the authorization header
        val response: HttpResponse = client.get("/shop-category?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":[{"id":"1","name":"Electronics"},{"id":"2","name":"Clothing"}]}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test updating shop category with authentication in engine`() = testApplication {
        val mockShopCategoryController = mockk<ShopCategoryController>(relaxed = true)

        // Mock the behavior of the updateShopCategory function
        coEvery { mockShopCategoryController.updateShopCategory(any(), any()) } returns ShopCategory(
            "1",
            "Updated Electronics"
        )

        application {
            testModule(mockShopCategoryController)
        }


        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val authHeader = request.headers[HttpHeaders.Authorization]
                    if (request.url.encodedPath == "/shop-category/1" && request.method == HttpMethod.Put &&
                        request.url.parameters["name"] == "Updated Electronics"
                    ) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.ADMIN.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }

                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":{"id":"1","name":"Updated Electronics"}}""",
                                    HttpStatusCode.OK
                                )
                            } else {
                                respond("Unauthorized", HttpStatusCode.Unauthorized)
                            }
                        } else {
                            respond("Unauthorized", HttpStatusCode.Unauthorized)
                        }
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }
        val jwtToken = generateJwtToken(RoleManagement.ADMIN.role) // Generate a token for ADMIN role

        // Send PUT request with the authorization header
        val response: HttpResponse = client.put("/shop-category/1?name=Updated Electronics") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"status":"success","data":{"id":"1","name":"Updated Electronics"}}""", response.bodyAsText())
    }

    @Test
    fun `test deleting shop category with authentication in engine`() = testApplication {
        val mockShopCategoryController = mockk<ShopCategoryController>(relaxed = true)

        // Mock the behavior of the deleteShopCategory function
        coEvery { mockShopCategoryController.deleteShopCategory(any()) } returns "1"

        application {
            testModule(mockShopCategoryController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val authHeader = request.headers[HttpHeaders.Authorization]
                    if (request.url.encodedPath == "/shop-category/1" && request.method == HttpMethod.Delete) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.ADMIN.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }

                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":"1"}""",
                                    HttpStatusCode.OK
                                )
                            } else {
                                respond("Unauthorized", HttpStatusCode.Unauthorized)
                            }
                        } else {
                            respond("Unauthorized", HttpStatusCode.Unauthorized)
                        }
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken(RoleManagement.ADMIN.role) // Generate a token for ADMIN role
        // Send DELETE request with the authorization header
        val response: HttpResponse = client.delete("/shop-category/1") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"status":"success","data":"1"}""", response.bodyAsText())
    }
}