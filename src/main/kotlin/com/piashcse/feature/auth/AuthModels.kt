package com.piashcse.feature.auth

import com.piashcse.model.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class OtpVerificationRequest(val userId: String, val otp: String)

@Serializable
data class SocialLoginRequest(val idToken: String, val mfaCode: String? = null)
