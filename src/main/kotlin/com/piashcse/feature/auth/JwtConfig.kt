package com.piashcse.feature.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.server.config.*
import java.util.*

object JwtConfig {
    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var algorithm: Algorithm
    private const val VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 hours in milliseconds

    lateinit var verifier: JWTVerifier
        private set

    fun init(environment: HoconApplicationConfig) {
        secret = environment.property("ktor.jwt.secret").getString()
        issuer = environment.property("ktor.jwt.issuer").getString()
        algorithm = Algorithm.HMAC512(secret)
        verifier = JWT.require(algorithm).withIssuer(issuer).build()
    }

    /**
     * Produce a token for the given JwtTokenBody
     */
    fun tokenProvider(jwtTokenBody: JwtTokenRequest): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("email", jwtTokenBody.email)
        .withClaim("userId", jwtTokenBody.userId)
        .withClaim("userType", jwtTokenBody.userType)
        .withExpiresAt(getExpiration())
        .sign(algorithm)

    private fun getExpiration() = Date(System.currentTimeMillis() + VALIDITY_MS)
}