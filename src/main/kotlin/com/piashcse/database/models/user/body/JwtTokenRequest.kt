package com.piashcse.database.models.user.body

import io.ktor.server.auth.*

data class JwtTokenRequest(val userId: String, val email: String, val userType: String) : Principal