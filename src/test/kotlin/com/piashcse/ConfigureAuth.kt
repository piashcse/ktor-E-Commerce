package com.piashcse

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthTest() {
    install(Authentication) {
        jwt("admin") {
            realm = "piashcse"
            verifier(
                jwtBuilder()
            )
            validate { credential ->
                // Accepting multiple roles
                val role = credential.payload.getClaim("role").asString()
                if (role in listOf("CUSTOMER", "SELLER", "ADMIN")) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
        jwt("seller") {
            realm = "piashcse"
            verifier(
                jwtBuilder()
            )
            validate { credential ->
                // Accepting multiple roles
                val role = credential.payload.getClaim("role").asString()
                if (role in listOf("CUSTOMER", "SELLER", "ADMIN")) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
        jwt("customer") {
            realm = "piashcse"
            verifier(
                jwtBuilder()
            )
            validate { credential ->
                // Accepting multiple roles
                val role = credential.payload.getClaim("role").asString()
                if (role in listOf("CUSTOMER", "SELLER", "ADMIN")) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
    }
}

private const val SECRET = "zAP5MBA4B4Ijz0MZaS48"
fun jwtBuilder(): JWTVerifier {
    return JWT.require(Algorithm.HMAC256(SECRET))
        .withIssuer("piashcse")
        .build()
}

fun generateJwtToken(role: String): String {
    return JWT.create()
        .withIssuer("piashcse")
        .withClaim("role", role)  // Pass appropriate role
        .withSubject("CUSTOMER")  // Set subject
        .sign(Algorithm.HMAC256(SECRET))
}