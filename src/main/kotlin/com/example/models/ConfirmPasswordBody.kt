package com.example.models

data class ConfirmPasswordBody(val email:String, val verificationCode:String, val password:String)
