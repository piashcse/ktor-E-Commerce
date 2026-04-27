package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class BrandResponse(val id: String, val name: String, val logo: String?)