package com.example.models.user

import com.example.entities.user.UsersResponse

data class LoginResponse(val user: UsersResponse?, val token:String)