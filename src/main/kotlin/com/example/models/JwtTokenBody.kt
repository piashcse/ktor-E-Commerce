package com.example.models

import io.ktor.auth.*

//data class JwtTokenBody(val userId: String, val email: String) : Principal
data class JwtTokenBody(val userId: String, val email: String, val userType: String) : Principal