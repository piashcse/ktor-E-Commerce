package com.piashcse.modules.auth.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.database.models.user.body.JwtTokenRequest
import java.util.Date

object JwtController {
    private const val SECRET = "zAP5MBA4B4Ijz0MZaS48"
    private const val ISSUER = "piashcse"
    private const val VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
    private val ALGORITHM = Algorithm.HMAC512(SECRET)

    val verifier: JWTVerifier by lazy {
        JWT
            .require(ALGORITHM)
            .withIssuer(ISSUER)
            .build()
    }

    /**
     * Produce a token for the given JwtTokenBody
     */
    fun tokenProvider(jwtTokenBody: JwtTokenRequest): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(ISSUER)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withClaim("userType", jwtTokenBody.userType)
        .withExpiresAt(getExpiration())
        .sign(ALGORITHM)

    /**
     * Calculate the expiration Date based on current time + the given validity
     */
    private fun getExpiration() = Date(System.currentTimeMillis() + VALIDITY_MS)
}