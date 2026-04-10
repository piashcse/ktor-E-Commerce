package com.piashcse.feature.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.config.DotEnvConfig
import com.piashcse.model.request.JwtTokenRequest
import java.util.*

/**
 * JWT configuration supporting both access tokens and refresh tokens.
 * Access tokens are short-lived (15 minutes default).
 * Refresh tokens are long-lived (7 days default) and stored in database.
 */
object JwtConfig {
    private lateinit var accessSecret: String
    private lateinit var refreshSecret: String
    private lateinit var issuer: String
    private lateinit var accessAlgorithm: Algorithm
    private lateinit var refreshAlgorithm: Algorithm

    // Token validity from environment (defaults: 15 min access, 7 days refresh)
    val accessTokenValidityMs: Long
        get() = DotEnvConfig.jwtAccessTokenValidityMs

    val refreshTokenValidityMs: Long
        get() = DotEnvConfig.jwtRefreshValidityMs

    lateinit var accessVerifier: JWTVerifier
        private set

    lateinit var refreshVerifier: JWTVerifier
        private set

    /**
     * Alias for backward compatibility - uses access token verifier
     */
    val verifier: JWTVerifier
        get() = accessVerifier

    fun init() {
        accessSecret = DotEnvConfig.jwtSecret
        refreshSecret = DotEnvConfig.jwtRefreshSecret
        issuer = DotEnvConfig.jwtIssuer
        accessAlgorithm = Algorithm.HMAC512(accessSecret)
        refreshAlgorithm = Algorithm.HMAC512(refreshSecret)

        accessVerifier = JWT.require(accessAlgorithm).withIssuer(issuer).build()
        refreshVerifier = JWT.require(refreshAlgorithm).withIssuer(issuer).build()
    }

    /**
     * Generate an access token (short-lived, used for API calls)
     */
    fun generateAccessToken(jwtTokenBody: JwtTokenRequest): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withClaim("userType", jwtTokenBody.userType)
        .withClaim("tokenType", "access")
        .withExpiresAt(getExpiration(accessTokenValidityMs))
        .sign(accessAlgorithm)

    /**
     * Generate a refresh token (long-lived, used to obtain new access tokens)
     */
    fun generateRefreshToken(jwtTokenBody: JwtTokenRequest): String = JWT.create()
        .withSubject("Refresh")
        .withIssuer(issuer)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withClaim("userType", jwtTokenBody.userType)
        .withClaim("tokenType", "refresh")
        .withExpiresAt(getExpiration(refreshTokenValidityMs))
        .sign(refreshAlgorithm)

    /**
     * Produce an access token for the given JwtTokenBody (backward compatibility alias)
     */
    fun tokenProvider(jwtTokenBody: JwtTokenRequest): String = generateAccessToken(jwtTokenBody)

    private fun getExpiration(validityMs: Long) = Date(System.currentTimeMillis() + validityMs)
}
