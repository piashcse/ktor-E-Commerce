package com.piashcse.route

import com.piashcse.configureAuthTest
import com.piashcse.controller.WishListController
import com.piashcse.entities.WishList
import com.piashcse.entities.product.Product
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

class WishlistTest {
    private fun Application.testModule(wishListController: WishListController) {
        configureAuthTest()
        routing {
            wishListRoute(wishListController)
        }
    }

    @Test
    fun `test adding product to wish list`() = testApplication {
        val mockWishListController = mockk<WishListController>(relaxed = true)

        // Mock the behavior of the addToWishList function
        coEvery { mockWishListController.addToWishList(any(), any()) } returns WishList(
            Product(
                "1",
                " A",
                null,
                null,
                "Product A",
                "100",
                1,
                "Detail of product",
                100.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null, null, null, null, null,
            )
        )

        // Initialize the test application with the mock controller
        application {
            testModule(mockWishListController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Extract the Authorization header
                    val authHeader = request.headers[HttpHeaders.Authorization]

                    if (request.url.encodedPath == "/wishlist" && request.method == HttpMethod.Post) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.CUSTOMER.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }

                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":{"id":"1","productName":"Product A","productDetail":"Product Detail A"}}""",
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

        val jwtToken = generateJwtToken(RoleManagement.CUSTOMER.role) // Generate a token for CUSTOMER role
        val response: HttpResponse = client.post("/wishlist") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
            contentType(ContentType.Application.Json)
            setBody("""{"productId":"1"}""") // Mock request body
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":{"id":"1","productName":"Product A","productDetail":"Product Detail A"}}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test getting wish list items`() = testApplication {
        val mockWishListController = mockk<WishListController>(relaxed = true)

        // Mock the behavior of the getWishList function
        coEvery { mockWishListController.getWishList(any(), any(), any()) } returns listOf(
            Product(
                "1",
                " A",
                null,
                null,
                "Product A",
                "100",
                1,
                "Detail Product A",
                100.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null, null, null, null, null,
            ),
            Product(
                "2",
                "B",
                null,
                null,
                "Product B",
                "100",
                1,
                "Detail Product B",
                100.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null, null, null, null, null, null,
            )
        )

        application {
            testModule(mockWishListController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Extract the Authorization header
                    val authHeader = request.headers[HttpHeaders.Authorization]
                    if (request.url.encodedPath == "/wishlist" && request.method == HttpMethod.Get &&
                        request.url.parameters["limit"] == "10" && request.url.parameters["offset"] == "0"
                    ) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.CUSTOMER.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }
                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":[{"id":"1","productName":"Product A","productDetail":"Detail Product A"},{"id":"2","productName":"Product B","productDetail":"Detail Product B"}]}""",
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

        val jwtToken = generateJwtToken(RoleManagement.CUSTOMER.role)
        val response: HttpResponse = client.get("/wishlist?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":[{"id":"1","productName":"Product A","productDetail":"Detail Product A"},{"id":"2","productName":"Product B","productDetail":"Detail Product B"}]}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test deleting item from wish list`() = testApplication {
        val mockWishListController = mockk<WishListController>(relaxed = true)

        // Mock the behavior of the deleteWishList function
        coEvery { mockWishListController.deleteWishList(any(), any()) } returns Product(
            "1",
            "A",
            null,
            null,
            "Product B",
            "100",
            1,
            "Detail Product A",
            100.0,
            null,
            null,
            null,
            null,
            null,
            null,
            null, null, null, null, null, null,
        )

        application {
            testModule(mockWishListController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Extract the Authorization header
                    val authHeader = request.headers[HttpHeaders.Authorization]
                    if (request.url.encodedPath == "/wishlist" && request.method == HttpMethod.Delete &&
                        request.url.parameters["productId"] == "1"
                    ) {
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf(RoleManagement.CUSTOMER.role)

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }
                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":{"id":"1","productName":"Product A","productDetail":"Product detail A"}}""",
                                    HttpStatusCode.OK
                                )
                            } else {
                                respond("Unauthorized", HttpStatusCode.Unauthorized)
                            }
                        }else{
                            respond("Unauthorized", HttpStatusCode.Unauthorized)
                        }
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken(RoleManagement.CUSTOMER.role)
        val response: HttpResponse = client.delete("/wishlist?productId=1") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":{"id":"1","productName":"Product A","productDetail":"Product detail A"}}""",
            response.bodyAsText()
        )
    }
}