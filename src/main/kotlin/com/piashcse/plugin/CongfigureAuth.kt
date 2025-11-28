package com.piashcse.plugin

import com.piashcse.constants.UserType
import com.piashcse.feature.auth.JwtConfig
import com.piashcse.model.request.JwtTokenRequest
import com.piashcse.utils.RoleHierarchy
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
        jwt (RoleManagement.CUSTOMER.role) {
            provideJwtAuthConfig(this, RoleManagement.CUSTOMER)
        }
        jwt(RoleManagement.SELLER.role) {
            provideJwtAuthConfig(this, RoleManagement.SELLER)
        }
        jwt(RoleManagement.ADMIN.role) {
            provideJwtAuthConfig(this, RoleManagement.ADMIN)
        }
        jwt(RoleManagement.SUPER_ADMIN.role) {
            provideJwtAuthConfig(this, RoleManagement.SUPER_ADMIN)
        }
    }
}

fun provideJwtAuthConfig(jwtConfig: JWTAuthenticationProvider.Config, userRole: RoleManagement) {
    jwtConfig.verifier(JwtConfig.verifier)
    jwtConfig.validate { call ->
        val userId = call.payload.getClaim("userId").asString()
        val email = call.payload.getClaim("email").asString()
        val userTypeString = call.payload.getClaim("userType").asString()

        // Convert string to UserType enum
        val userType = try {
            UserType.valueOf(userTypeString.uppercase())
        } catch (e: IllegalArgumentException) {
            return@validate null
        }

        // Check if the user has access to the required role (role hierarchy)
        if (RoleHierarchy.hasAccess(userType, userRole.toUserType())) {
            JwtTokenRequest(userId, email, userTypeString)
        } else null
    }
}

enum class RoleManagement(val role: String) {
    SUPER_ADMIN("super_admin"),
    ADMIN("admin"),
    SELLER("seller"),
    CUSTOMER("customer");

    fun toUserType(): UserType = when (this) {
        SUPER_ADMIN -> UserType.SUPER_ADMIN
        ADMIN -> UserType.ADMIN
        SELLER -> UserType.SELLER
        CUSTOMER -> UserType.CUSTOMER
    }

    companion object {
        fun fromUserType(userType: UserType): RoleManagement = when (userType) {
            UserType.SUPER_ADMIN -> SUPER_ADMIN
            UserType.ADMIN -> ADMIN
            UserType.SELLER -> SELLER
            UserType.CUSTOMER -> CUSTOMER
        }
    }
}