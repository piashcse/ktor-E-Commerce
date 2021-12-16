package com.example.models.user

import io.ktor.auth.*
data class JwtTokenBody(val userId: String, val email: String, val userType: String) : Principal