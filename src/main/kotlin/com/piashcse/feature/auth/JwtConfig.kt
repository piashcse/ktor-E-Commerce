package com.piashcse.feature.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.config.DotEnvConfig
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.server.config.*
import java.util.*

object JwtConfig {
    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var algorithm: Algorithm
    const val ACCESS_TOKEN_VALIDITY_MS = 30 * 60 * 1000L // 30 minutes in milliseconds
    const val REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
    
    // Maximum refresh token lifetime (30 days) - for sliding expiration
    const val MAX_REFRESH_TOKEN_LIFETIME_MS = 30 * 24 * 60 * 60 * 1000L // 30 days

    lateinit var verifier: JWTVerifier
        private set

    fun init() {
        secret = DotEnvConfig.jwtSecret
        issuer = DotEnvConfig.jwtIssuer
        algorithm = Algorithm.HMAC512(secret)
        verifier = JWT.require(algorithm).withIssuer(issuer).build()
    }

    /**
     * Produce an access token for the given JwtTokenBody
     */
    fun tokenProvider(jwtTokenBody: JwtTokenRequest): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withClaim("userType", jwtTokenBody.userType)
        .withExpiresAt(getAccessTokenExpiration())
        .sign(algorithm)

    // Note: Refresh tokens are now random strings stored in the database
    // This method is kept for backward compatibility but should not be used for refresh tokens
    @Deprecated("Use generateRefreshToken() function instead for refresh tokens")
    fun refreshTokenProvider(jwtTokenBody: JwtTokenRequest): String = JWT.create()
        .withSubject("Refresh Authentication")
        .withIssuer(issuer)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withClaim("userType", jwtTokenBody.userType)
        .withExpiresAt(getRefreshTokenExpiration())
        .sign(algorithm)

    private fun getAccessTokenExpiration() = Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_MS)

    private fun getRefreshTokenExpiration() = Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY_MS)
}