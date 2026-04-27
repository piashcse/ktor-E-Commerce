package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(val id: String, val email: String, val message: String)