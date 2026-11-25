package com.piashcse.feature.kyc

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class KycRejectRequest(val reason: String)
