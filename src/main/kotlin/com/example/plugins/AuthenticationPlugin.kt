package com.example.plugins

import com.example.models.user.body.JwtTokenBody
import com.example.controller.JwtController
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureAuthentication() {
    install(Authentication) {
        /**
         * Setup the JWT authentication to be used in [Routing].
         * If the token is valid, the corresponding [User] is fetched from the database.
         * The [User] can then be accessed in each [ApplicationCall].
         */
        jwt {
            provideJwtAuthConfig(this, RoleManagement.USER)
        }
        jwt(RoleManagement.ADMIN.role) {
            provideJwtAuthConfig(this, RoleManagement.ADMIN)
        }
        jwt(RoleManagement.SELLER.role) {
            provideJwtAuthConfig(this, RoleManagement.SELLER)
        }
    }
}

fun provideJwtAuthConfig(jwtConfig: JWTAuthenticationProvider.Config, userRole: RoleManagement) {
    jwtConfig.verifier(JwtController.verifier)
    jwtConfig.realm = "ktor.io"
    jwtConfig.validate {
        val userId = it.payload.getClaim("userId").asString()
        val email = it.payload.getClaim("email").asString()
        val userType = it.payload.getClaim("userType").asString()
        if (userType == userRole.role) {
            JwtTokenBody(userId, email, userType)
        } else null
    }
}

enum class RoleManagement(val role: String) {
    SUPER_ADMIN("super_admin"),
    ADMIN("admin"),
    SELLER("seller"),
    USER("user")
}