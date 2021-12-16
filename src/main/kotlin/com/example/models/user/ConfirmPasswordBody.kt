package com.example.models.user

data class ConfirmPasswordBody(val email:String, val verificationCode:String, val password:String)
