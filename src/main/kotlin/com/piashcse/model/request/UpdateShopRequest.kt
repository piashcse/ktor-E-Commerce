package com.piashcse.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateShopRequest(
    val name: String? = null,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val logo: String? = null,
    val coverImage: String? = null
)