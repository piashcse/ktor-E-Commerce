package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.GiftCardStatus
import com.piashcse.constants.ReferralStatus
import com.piashcse.feature.loyalty.GiftCardResponse
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.loyalty.ReferralResponse
import com.piashcse.feature.loyalty.WalletResponse
import com.piashcse.feature.loyalty.giftCardAdminRoutes
import com.piashcse.feature.loyalty.referralRoutes
import com.piashcse.feature.loyalty.walletAdminRoutes
import com.piashcse.feature.loyalty.walletRoutes
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

class LoyaltyRoutesTest {
    private val loyalty: LoyaltyService = mockk()

    private fun wallet() = WalletResponse("25.00", 150, 200, "REF12345")

    private fun referral() =
        ReferralResponse(
            id = "ref-1",
            referrerUserId = "user-2",
            refereeUserId = "user-1",
            code = "REF12345",
            status = ReferralStatus.PENDING,
            rewardPoints = 100,
            rewardCredit = "0.00",
            completedAt = null,
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun giftCard() =
        GiftCardResponse(
            id = "gc-1",
            code = "GIFT1234",
            initialBalance = "50.00",
            balance = "50.00",
            currency = "USD",
            status = GiftCardStatus.ACTIVE,
            expiresAt = null,
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { loyalty } })
            routing {
                route("/api/v1/wallet") { walletRoutes() }
                route("/api/v1/referrals") { referralRoutes() }
                route("/api/v1/admin/gift-cards") { adminAuth { giftCardAdminRoutes() } }
                route("/api/v1/admin/wallets") { adminAuth { walletAdminRoutes() } }
            }
        }
    }

    @Test
    fun `customer reads wallet`() =
        testApplication {
            coEvery { loyalty.wallet("user-1") } returns wallet()
            setup()
            val res =
                client.get("/api/v1/wallet") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "REF12345")
        }

    @Test
    fun `customer claims a referral`() =
        testApplication {
            coEvery { loyalty.claimReferral("user-1", "REF12345") } returns referral()
            setup()
            val res =
                client.post("/api/v1/referrals/claim") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody("""{"code":"REF12345"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "ref-1")
        }

    @Test
    fun `admin issues a gift card`() =
        testApplication {
            coEvery { loyalty.issueGiftCard("admin-1", any()) } returns giftCard()
            setup()
            val res =
                client.post("/api/v1/admin/gift-cards") {
                    header(HttpHeaders.Authorization, authHeader(userId = "admin-1", userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"amount":50.00,"currency":"USD"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertContains(res.bodyAsText(), "GIFT1234")
        }

    @Test
    fun `admin credits a wallet`() =
        testApplication {
            coEvery { loyalty.adminCredit("user-1", any()) } returns wallet()
            setup()
            val res =
                client.post("/api/v1/admin/wallets/user-1/credit") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"amount":25.00,"note":"Goodwill"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "25.00")
        }

    @Test
    fun `customer can check a gift-card balance`() =
        testApplication {
            coEvery { loyalty.giftCard("GIFT1234") } returns giftCard()
            setup()
            val res =
                client.get("/api/v1/wallet/gift-cards/GIFT1234") {
                    header(HttpHeaders.Authorization, authHeader())
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "50.00")
        }

    @Test
    fun `customers cannot issue gift cards`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/gift-cards") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"amount":50.00}""")
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
