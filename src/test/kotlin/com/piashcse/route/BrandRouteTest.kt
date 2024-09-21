import com.piashcse.controller.BrandController
import com.piashcse.entities.product.Brand
import com.piashcse.route.brandRoute
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import com.piashcse.configureAuthTest
import com.piashcse.generateJwtToken

class BrandRouteTest {
    private fun Application.testModule(brandController: BrandController) {
        configureAuthTest()
        routing {
            authenticate("admin", "customer", "seller") {
                brandRoute(brandController)
            }
        }
    }

    private suspend fun validateBrandResponse(role: String, client: HttpClient) {
        val token = generateJwtToken(role) // Generate JWT token with the given role
        val response: HttpResponse = client.get("/brand?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        // Validate the response
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":[{"id":"1","brandName":"Brand A","brandLogo":"logoA.png"},{"id":"2","brandName":"Brand B","brandLogo":"logoB.png"}]}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test get routing for Brand as ADMIN, SELLER, CUSTOMER`() = testApplication {
        val mockBrandController = mockk<BrandController>(relaxed = true)
        val brands = listOf(
            Brand("1", "Brand A", "logoA.png"),
            Brand("2", "Brand B", "logoB.png")
        )

        coEvery { mockBrandController.getBrands(any(), any()) } returns brands

        // Initialize the test application with the mock controller
        application {
            testModule(mockBrandController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Extract the Authorization header
                    val authHeader = request.headers[HttpHeaders.Authorization]

                    // Example for GET request with dynamic role handling
                    if (request.url.encodedPath == "/brand" && request.method == HttpMethod.Get &&
                        request.url.parameters["limit"] == "10" && request.url.parameters["offset"] == "0"
                    ) {
                        // Validate the Authorization header
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Here, you can dynamically generate tokens for different roles
                            val validRoles = listOf("ADMIN", "CUSTOMER", "SELLER")

                            // Loop through valid roles and check if the provided token matches any valid one
                            val isValidToken = validRoles.any { role ->
                                val generatedToken = generateJwtToken(role)
                                token == generatedToken
                            }

                            if (isValidToken) {
                                respond(
                                    """{"status":"success","data":[{"id":"1","brandName":"Brand A","brandLogo":"logoA.png"},{"id":"2","brandName":"Brand B","brandLogo":"logoB.png"}]}""",
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

        validateBrandResponse("CUSTOMER", client)
        validateBrandResponse("ADMIN", client)
        validateBrandResponse("SELLER", client)
    }


    @Test
    fun `test post routing for Brand as ADMIN`() = testApplication {
        val mockBrandController = mockk<BrandController>(relaxed = true)

        // Mock the behavior of the addBrand function
        coEvery { mockBrandController.addBrand(any()) } returns Brand("1", "Brand Added Successfully", null)

        // Initialize the test application with the mock controller
        application {
            testModule(mockBrandController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    // Extract the Authorization header
                    val authHeader = request.headers[HttpHeaders.Authorization]

                    if (request.url.encodedPath == "/brand" && request.method == HttpMethod.Post) {
                        // Validate the Authorization header
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // For simplicity, you can check if the token matches a pre-generated token
                            // In a real-world scenario, you could mock the token validation logic.
                            val validToken = generateJwtToken("ADMIN") // Assuming a valid token for the test case

                            if (token == validToken) {
                                respond(
                                    """{"status":"success","data":"Brand Added Successfully"}""",
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

        val jwtToken = generateJwtToken("ADMIN") // Generate JWT token for ADMIN role
        val response: HttpResponse = client.post("/brand") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
            contentType(ContentType.Application.Json)
            setBody("""{"brandName":"Brand C"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":"Brand Added Successfully"}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test put routing for Brand as ADMIN`() = testApplication {
        val mockBrandController = mockk<BrandController>(relaxed = true)

        // Mock the behavior of the updateBrand function
        coEvery { mockBrandController.updateBrand(any(), any()) } returns Brand(
            id = "1",
            brandName = "Brand Updated Successfully",
            brandLogo = null
        )

        // Initialize the test application with the mock controller
        application {
            testModule(mockBrandController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.encodedPath == "/brand/1" && request.method == HttpMethod.Put) {
                        val authHeader = request.headers[HttpHeaders.Authorization]

                        // Check if the Authorization header contains the Bearer token
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Mock token validation logic
                            val sellerToken = generateJwtToken("ADMIN")

                            if (token == sellerToken) {
                                respond(
                                    """{"status":"success","data":"Brand Updated Successfully"}""",
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

        // Generate the JWT token for SELLER
        val jwtToken = generateJwtToken("ADMIN")

        // Perform PUT request to update the brand
        val response: HttpResponse = client.put("/brand/1") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken") // Set the JWT token
            contentType(ContentType.Application.Json) // Specify content type
            setBody("""{"brandName":"Brand Updated"}""") // Set the request body
        }

        // Validate the response
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":"Brand Updated Successfully"}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test delete routing for Brand as ADMIN`() = testApplication {
        val mockBrandController = mockk<BrandController>(relaxed = true)

        // Mock the behavior of the deleteBrand function
        coEvery { mockBrandController.deleteBrand(any()) } returns "Brand Deleted Successfully"

        // Initialize the test application with the mock controller
        application {
            testModule(mockBrandController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.encodedPath == "/brand/1" && request.method == HttpMethod.Delete) {
                        val authHeader = request.headers[HttpHeaders.Authorization]

                        // Check if the Authorization header contains the Bearer token
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            val token = authHeader.removePrefix("Bearer ")

                            // Mock token validation logic
                            val adminToken = generateJwtToken("ADMIN")

                            if (token == adminToken) {
                                respond(
                                    """{"status":"success","data":"Brand Deleted Successfully"}""",
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

        // Generate the JWT token for ADMIN
        val jwtToken = generateJwtToken("ADMIN")

        // Perform DELETE request to delete the brand
        val response: HttpResponse = client.delete("/brand/1") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken") // Set the JWT token
        }

        // Validate the response
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":"Brand Deleted Successfully"}""",
            response.bodyAsText()
        )
    }
}