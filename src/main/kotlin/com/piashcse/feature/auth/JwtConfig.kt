package com.piashcse.feature.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.config.DotEnvConfig
import com.piashcse.model.request.JwtTokenRequest
import java.util.*

object JwtConfig {
    private val secret = DotEnvConfig.jwtSecret
    private val issuer = DotEnvConfig.jwtIssuer
    private val audience = DotEnvConfig.jwtAudience
    private val algorithm = Algorithm.HMAC512(secret)
    private val validityMs = 15 * 60 * 1000L

    val verifier: JWTVerifier =
        JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build()

    fun tokenProvider(jwtTokenBody: JwtTokenRequest): String =
        JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("email", jwtTokenBody.email)
            .withClaim("userId", jwtTokenBody.userId)
            .withClaim("userType", jwtTokenBody.userType)
            .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
            .sign(algorithm)
}
