package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.KycStatus
import com.piashcse.feature.kyc.KycService
import com.piashcse.feature.kyc.SellerKycResponse
import com.piashcse.feature.kyc.adminKycRoutes
import com.piashcse.feature.kyc.sellerKycRoutes
import com.piashcse.plugin.adminAuth
import com.piashcse.plugin.sellerAuth
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
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

class KycRoutesTest {
    private val service: KycService = mockk()

    private fun sample() =
        SellerKycResponse(
            sellerId = "seller-1",
            userId = "user-1",
            email = "seller@example.com",
            businessName = "ACME",
            businessRegistrationNumber = null,
            taxId = null,
            bankName = null,
            kycStatus = KycStatus.PENDING,
            documentUrl = null,
            submittedAt = null,
            verifiedAt = null,
            rejectionReason = null,
            shopStatus = "APPROVED",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                route("/api/v1/seller/kyc") { sellerAuth { sellerKycRoutes() } }
                route("/api/v1/admin/sellers") { adminAuth { adminKycRoutes() } }
            }
        }
    }

    @Test
    fun `seller can read own status`() =
        testApplication {
            coEvery { service.get("user-1") } returns sample()
            setup()
            val res =
                client.get("/api/v1/seller/kyc") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "ACME")
        }

    @Test
    fun `seller can submit kyc`() =
        testApplication {
            coEvery { service.submit("user-1", any()) } returns sample()
            setup()
            val res =
                client.post("/api/v1/seller/kyc") {
                    header(HttpHeaders.Authorization, authHeader(userType = "SELLER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"businessName":"ACME"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
        }

    @Test
    fun `customer cannot access seller kyc`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/seller/kyc") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `admin can list submissions`() =
        testApplication {
            coEvery { service.list(null, 20, 0) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(totalCount = 1, limit = 20, offset = 0))
            setup()
            val res =
                client.get("/api/v1/admin/sellers") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "seller-1")
        }
}
