package com.piashcse

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.RouteTestHelper.jsonClient
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.feature.auth.UserAuthenticationService
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.cart.CartRepository
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.request.CartRequest
import com.piashcse.model.request.LoginRequest
import com.piashcse.model.request.PaymentRequest
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.RegisterRequest
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.PaymentResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Ktor-docs-aligned contracts: typed request bodies, typed response bodies,
 * and MockK interaction verification (per MockK docs: coEvery/coVerify).
 */
class TypedContractTest {
    @Test
    fun `register typed round-trip with verification`() =
        testApplication {
            val authRepo: AuthRepository = mockk()
            coEvery { authRepo.register(any()) } returns
                com.piashcse.model.response.RegistrationResult.Created("user-1", "test@example.com", "OTP sent")
            coEvery { authRepo.getRegistrationOtp(any()) } returns "123456"
            application {
                installTestInfra(
                    org.koin.dsl.module {
                        single<AuthRepository> { authRepo }
                        single { UserAuthenticationService(get()) }
                    },
                )
                routing { route("/api/v1/auth") { authRoutes() } }
            }
            val client = jsonClient()
            val res =
                client.post("/api/v1/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest("test@example.com", "Password1!", "customer"))
                }
            assertEquals(HttpStatusCode.Created, res.status)
            val body = res.body<com.piashcse.model.response.RegistrationResult.Created>()
            assertIs<com.piashcse.model.response.RegistrationResult.Created>(body)
            assertEquals("user-1", body.id)
            coVerify(exactly = 1) { authRepo.register(match { it.email == "test@example.com" }) }
        }

    @Test
    fun `login typed round-trip with verification`() =
        testApplication {
            val authRepo: AuthRepository = mockk()
            val hash =
                at.favre.lib.crypto.bcrypt.BCrypt.withDefaults()
                    .hashToString(4, "Password1!".toCharArray())
            val user =
                mockk<UserDAO> {
                    every { id } returns EntityID("user-1", UserTable)
                    every { email } returns "test@example.com"
                    every { password } returns hash
                    every { userType } returns com.piashcse.constants.UserType.CUSTOMER
                    every { isVerified } returns true
                    every { isActive } returns true
                    every { createdAt } returns LocalDateTime.of(2024, 1, 1, 0, 0)
                    every { updatedAt } returns null
                }
            coEvery { authRepo.getLoginAttempt(any(), any()) } returns null
            coEvery { authRepo.findUserByEmailAndType(any(), any()) } returns user
            coEvery { authRepo.resetLoginAttempts(any(), any()) } returns Unit
            coEvery { authRepo.generateTokenPair(any(), any(), any()) } returns
                com.piashcse.model.request.TokenPair("access-1", "refresh-1", "Bearer", 900)
            coEvery { authRepo.storeRefreshToken(any(), any()) } returns Unit
            application {
                installTestInfra(
                    org.koin.dsl.module {
                        single<AuthRepository> { authRepo }
                        single { UserAuthenticationService(get()) }
                    },
                )
                routing { route("/api/v1/auth") { authRoutes() } }
            }
            val client = jsonClient()
            val res =
                client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest("test@example.com", "Password1!", "customer"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.body<com.piashcse.database.entities.LoginResponse>()
            assertEquals("access-1", body.accessToken)
            coVerify(exactly = 1) { authRepo.storeRefreshToken("user-1", "refresh-1") }
        }

    @Test
    fun `payment typed round-trip enforces caller ownership`() =
        testApplication {
            val repo: PaymentRepository = mockk()
            coEvery { repo.createPayment(any(), any()) } returns
                PaymentResponse("pay-1", "o-1", "115.00", PaymentStatus.COMPLETED, PaymentMethod.COD, "tx-1")
            application {
                installTestInfra(org.koin.dsl.module { single<PaymentRepository> { repo } })
                routing { route("/api/v1/payments") { paymentRoutes() } }
            }
            val client = jsonClient()
            val res =
                client.post("/api/v1/payments") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody(PaymentRequest("o-1", BigDecimal("115.00"), PaymentStatus.PENDING, PaymentMethod.COD, "tx-1"))
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertEquals("pay-1", res.body<PaymentResponse>().id)
            // Security: caller id must be threaded through to the repo.
            coVerify(exactly = 1) { repo.createPayment(any(), "user-1") }
        }

    @Test
    fun `order cancel typed round-trip verifies actor`() =
        testApplication {
            val repo: OrderRepository = mockk()
            coEvery { repo.cancelOrder(any(), any(), any(), any()) } returns
                OrderResponse(orderId = "o-1", orderNumber = "ORD-1", subTotal = "100.00", total = "115.00", status = OrderStatus.CANCELED)
            application {
                installTestInfra(org.koin.dsl.module { single<OrderRepository> { repo } })
                routing { route("/api/v1/orders") { orderRoutes() } }
            }
            val client = jsonClient()
            val res =
                client.post("/api/v1/orders/o-1/cancel") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody(CancelOrderRequest("changed mind"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals(OrderStatus.CANCELED, res.body<OrderResponse>().status)
            coVerify(exactly = 1) { repo.cancelOrder("o-1", "user-1", "changed mind", any()) }
        }

    @Test
    fun `cart create typed round-trip verifies owner and qty`() =
        testApplication {
            val repo: CartRepository = mockk()
            coEvery { repo.createCart(any(), any(), any()) } returns
                com.piashcse.database.entities.Cart("p-1", 2, null)
            application {
                installTestInfra(org.koin.dsl.module { single<CartRepository> { repo } })
                routing { route("/api/v1/carts") { cartRoutes() } }
            }
            val client = jsonClient()
            val res =
                client.post("/api/v1/carts") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody(CartRequest("p-1", 2))
                }
            assertEquals(HttpStatusCode.Created, res.status)
            assertEquals("p-1", res.body<com.piashcse.database.entities.Cart>().productId)
            coVerify(exactly = 1) { repo.createCart("user-1", "p-1", 2) }
        }

    @Test
    fun `refund create typed round-trip verifies owner`() =
        testApplication {
            val repo: RefundRequestRepository = mockk()
            coEvery { repo.createRefundRequest(any(), any(), any()) } returns mockRefund()
            application {
                installTestInfra(org.koin.dsl.module { single<RefundRequestRepository> { repo } })
                routing { route("/api/v1/refund-requests") { refundRequestRoutes() } }
            }
            val client = jsonClient()
            val res =
                client.post("/api/v1/refund-requests/o-1") {
                    header(HttpHeaders.Authorization, authHeader())
                    contentType(ContentType.Application.Json)
                    setBody(RefundRequestRequest("oi-1", "damaged"))
                }
            assertEquals(HttpStatusCode.Created, res.status)
            coVerify(exactly = 1) { repo.createRefundRequest("user-1", "o-1", any()) }
        }

    private fun mockRefund() =
        com.piashcse.model.response.RefundRequestResponse(
            "r-1", "oi-1", "o-1", "user-1", "damaged", null,
            com.piashcse.constants.RefundStatus.PENDING, null, null, null,
            "2024-01-01T00:00:00", null, "2024-01-01T00:00:00", "2024-01-01T00:00:00",
        )
}
