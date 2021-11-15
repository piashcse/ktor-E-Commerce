package com.example.models

import com.example.entities.UsersResponse

data class LoginResponse(val user: UsersResponse?, val token:String)