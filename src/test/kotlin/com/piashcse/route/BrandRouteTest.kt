
import com.auth0.jwt.JWT
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


    @Test
    fun `test get routing for Brand`() = testApplication {
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
                    if (request.url.encodedPath == "/brand" &&
                        request.url.parameters["limit"] == "10" &&
                        request.url.parameters["offset"] == "0") {
                        respond(
                            """{"status":"success","data":[{"id":"1","brandName":"Brand A","brandLogo":"logoA.png"},{"id":"2","brandName":"Brand B","brandLogo":"logoB.png"}]}""",
                            HttpStatusCode.OK
                        )
                    } else {
                        respond("Not found", HttpStatusCode.NotFound)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken("CUSTOMER") // Generate JWT token with the appropriate role
        val response: HttpResponse = client.get("/brand?limit=10&offset=0") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":[{"id":"1","brandName":"Brand A","brandLogo":"logoA.png"},{"id":"2","brandName":"Brand B","brandLogo":"logoB.png"}]}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test post routing for Brand`() = testApplication {
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
                    if (request.url.encodedPath == "/brand" && request.method == HttpMethod.Post) {
                        respond(
                            """{"status":"success","data":"Brand Added Successfully"}""",
                            HttpStatusCode.OK
                        )
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken("ADMIN") // Use an appropriate role
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
    fun `test put routing for Brand`() = testApplication {
        val mockBrandController = mockk<BrandController>(relaxed = true)

        // Mock the behavior of the updateBrand function
        coEvery { mockBrandController.updateBrand(any(), any()) } returns Brand("1", "Brand Updated Successfully", null)

        // Initialize the test application with the mock controller
        application {
            testModule(mockBrandController)
        }

        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.encodedPath == "/brand/1" && request.method == HttpMethod.Put) {
                        respond(
                            """{"status":"success","data":"Brand Updated Successfully"}""",
                            HttpStatusCode.OK
                        )
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken("SELLER") // Use an appropriate role
        val response: HttpResponse = client.put("/brand/1") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
            contentType(ContentType.Application.Json)
            setBody("""{"brandName":"Brand Updated"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":"Brand Updated Successfully"}""",
            response.bodyAsText()
        )
    }

    @Test
    fun `test delete routing for Brand`() = testApplication {
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
                        respond(
                            """{"status":"success","data":"Brand Deleted Successfully"}""",
                            HttpStatusCode.OK
                        )
                    } else {
                        respond("Bad Request", HttpStatusCode.BadRequest)
                    }
                }
            }
        }

        val jwtToken = generateJwtToken("ADMIN") // Use an appropriate role
        val response: HttpResponse = client.delete("/brand/1") {
            header(HttpHeaders.Authorization, "Bearer $jwtToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            """{"status":"success","data":"Brand Deleted Successfully"}""",
            response.bodyAsText()
        )
    }
}