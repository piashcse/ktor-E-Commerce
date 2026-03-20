package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class Registration(val id: String, val email: String, val message: String)