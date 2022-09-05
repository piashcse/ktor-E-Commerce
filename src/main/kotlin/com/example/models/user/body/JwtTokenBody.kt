package com.example.models.user.body

import io.ktor.server.auth.*

data class JwtTokenBody(val userId: String, val email: String, val userType: String) : Principal