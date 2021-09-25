package com.example.models

import io.ktor.auth.*

data class LoginBody(val email:String, val password:String): Principal
