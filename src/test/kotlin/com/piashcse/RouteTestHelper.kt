package com.piashcse

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.constants.AppConstants
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.plugin.BigDecimalSerializer
import com.piashcse.plugin.LocalDateTimeSerializer
import com.piashcse.plugin.RateLimitNames
import com.piashcse.plugin.configureStatusPage
import com.piashcse.plugin.installRequestTracing
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.koin.core.module.Module
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import kotlin.time.Duration.Companion.minutes

/** Shared harness for DB-free route tests. Repos are MockK fakes; auth is a test HMAC JWT. */
object RouteTestHelper {
    const val ISSUER = "test-issuer"
    const val AUDIENCE = "test-audience"
    private const val SECRET = "test-secret-at-least-32-characters-long!!"
    private val algorithm = Algorithm.HMAC512(SECRET)

    fun token(
        userId: String = "user-1",
        email: String = "test@example.com",
        userType: String = "CUSTOMER",
    ): String =
        JWT.create()
            .withSubject("Authentication")
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("email", email)
            .withClaim("userId", userId)
            .withClaim("userType", userType)
            .sign(algorithm)

    fun Application.installTestInfra(koinModule: Module) {
        installRequestTracing()
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    prettyPrint = true
                    serializersModule =
                        SerializersModule {
                            contextual(LocalDateTimeSerializer)
                            contextual(BigDecimalSerializer)
                        }
                },
            )
        }
        install(Authentication) {
            jwt(AppConstants.Authentication.JWT_AUTHENTICATOR) {
                verifier(JWT.require(algorithm).withIssuer(ISSUER).withAudience(AUDIENCE).build())
                validate { credential ->
                    val userId = credential.payload.getClaim("userId").asString()
                    val email = credential.payload.getClaim("email").asString()
                    val userType = credential.payload.getClaim("userType").asString()
                    if (userId.isNullOrBlank() || email.isNullOrBlank() || userType.isNullOrBlank()) {
                        null
                    } else {
                        JwtTokenRequest(userId, email, userType)
                    }
                }
            }
        }
        install(RateLimit) {
            listOf(
                RateLimitNames.AUTH,
                RateLimitNames.OTP,
                RateLimitNames.REFRESH_TOKEN,
                RateLimitNames.WRITE,
                RateLimitNames.SEARCH,
                RateLimitNames.SELLER_WRITE,
                RateLimitNames.ADMIN_WRITE,
                RateLimitNames.GENERAL,
            ).forEach { name ->
                register(RateLimitName(name)) {
                    rateLimiter(limit = 10_000, refillPeriod = 1.minutes)
                    requestKey { "test-key" }
                }
            }
        }
        configureStatusPage()
        install(Koin) {
            modules(koinModule)
        }
    }

    fun ApplicationTestBuilder.authHeader(
        userId: String = "user-1",
        email: String = "test@example.com",
        userType: String = "CUSTOMER",
    ): String = "Bearer ${token(userId, email, userType)}"

    /**
     * JSON client per Ktor docs (createClient + ContentNegotiation) for typed
     * request bodies and typed response assertions.
     */
    fun ApplicationTestBuilder.jsonClient(): HttpClient =
        createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        serializersModule =
                            SerializersModule {
                                contextual(LocalDateTimeSerializer)
                                contextual(BigDecimalSerializer)
                            }
                    },
                )
            }
        }

    fun stopKoin(app: Application) {
        runCatching { app.getKoin().close() }
    }
}
