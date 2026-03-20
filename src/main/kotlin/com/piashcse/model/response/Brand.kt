package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class Brand(val id: String, val name: String, val logo: String?)