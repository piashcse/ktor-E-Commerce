package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ShopRequest(
    val name: String,
    val categoryId: String,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val logo: String? = null,
    val coverImage: String? = null,
) {
    init {
        validate(this) {
            validate(ShopRequest::name).isNotNull().isNotEmpty()
            validate(ShopRequest::categoryId).isNotNull().isNotEmpty()
        }
    }
}
