package com.piashcse

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.piashcse.plugins.RoleManagement
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuthTest() {
    val authRealm = "piashcse"
    install(Authentication) {
        jwt(RoleManagement.ADMIN.role) {
            realm = authRealm
            verifier(
                jwtBuilder()
            )
            validate { credential ->
                // Accepting multiple roles
                val role = credential.payload.getClaim("role").asString()
                if (role in listOf("ADMIN")) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
        jwt(RoleManagement.SELLER.role) {
            realm = authRealm
            verifier(
                jwtBuilder()
            )
            validate { credential ->
                // Accepting multiple roles
                val role = credential.payload.getClaim("role").asString()
                if (role in listOf("SELLER")) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
        jwt(RoleManagement.CUSTOMER.role) {
            realm = authRealm
            verifier(
                jwtBuilder()
            )
            validate { credential ->
                // Accepting multiple roles
                val role = credential.payload.getClaim("role").asString()
                if (role in listOf("CUSTOMER")) {
                    UserIdPrincipal(credential.payload.subject)
                } else {
                    null
                }
            }
        }
    }
}

private const val SECRET = "zAP5MBA4B4Ijz0MZaS48"
private const val ISSUER = "piashcse"
fun jwtBuilder(): JWTVerifier {
    return JWT.require(Algorithm.HMAC256(SECRET))
        .withIssuer(ISSUER)
        .build()
}

fun generateJwtToken(role: String): String {
    return JWT.create()
        .withIssuer(ISSUER)
        .withClaim("role", role)  // Pass appropriate role
        .withSubject("CUSTOMER")  // Set subject
        .sign(Algorithm.HMAC256(SECRET))
}