package com.piashcse.plugins

import com.piashcse.modules.auth.controller.JwtController
import com.piashcse.database.models.user.body.JwtTokenRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    install(Authentication) {
        /**
         * Setup the JWT authentication to be used in [Routing].
         * If the token is valid, the corresponding [User] is fetched from the database.
         * The [User] can then be accessed in each [ApplicationCall].
         */
        jwt (RoleManagement.CUSTOMER.role){
            provideJwtAuthConfig(this, RoleManagement.CUSTOMER)
        }
        jwt(RoleManagement.ADMIN.role) {
            provideJwtAuthConfig(this, RoleManagement.ADMIN)
        }
        jwt(RoleManagement.SELLER.role) {
            provideJwtAuthConfig(this, RoleManagement.SELLER)
        }
        jwt(RoleManagement.SUPER_ADMIN.role) {
            provideJwtAuthConfig(this, RoleManagement.SUPER_ADMIN)
        }
    }
}

fun provideJwtAuthConfig(jwtConfig: JWTAuthenticationProvider.Config, userRole: RoleManagement) {
    jwtConfig.verifier(JwtController.verifier)
    jwtConfig.realm = "piashcse"
    jwtConfig.validate {
        val userId = it.payload.getClaim("userId").asString()
        val email = it.payload.getClaim("email").asString()
        val userType = it.payload.getClaim("userType").asString()
        if (userType == userRole.role) {
            JwtTokenRequest(userId, email, userType)
        } else null
    }
}

enum class RoleManagement(val role: String) {
    SUPER_ADMIN("super_admin"),
    ADMIN("admin"),
    SELLER("seller"),
    CUSTOMER("customer")
}