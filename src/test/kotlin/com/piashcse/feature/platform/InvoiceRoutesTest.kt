package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.UserType
import com.piashcse.feature.invoice.InvoiceService
import com.piashcse.feature.invoice.orderDocumentRoutes
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class InvoiceRoutesTest {
    private val service: InvoiceService = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing { route("/api/v1/orders") { orderDocumentRoutes() } }
        }
    }

    @Test
    fun `customer can print an invoice`() =
        testApplication {
            coEvery { service.invoice("o-1", "user-1", UserType.CUSTOMER) } returns "<html><body>Invoice ORD-1</body></html>"
            setup()
            val res =
                client.get("/api/v1/orders/o-1/invoice") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Invoice ORD-1")
        }

    @Test
    fun `seller can print a packing slip`() =
        testApplication {
            coEvery { service.packingSlip("o-1", "user-1", UserType.SELLER) } returns "<html><body>Packing ORD-1</body></html>"
            setup()
            val res =
                client.get("/api/v1/orders/o-1/packing-slip") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Packing ORD-1")
        }

    @Test
    fun `anonymous cannot print an invoice`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/orders/o-1/invoice").status)
        }
}
