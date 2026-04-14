package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

@Serializable
data class UpdateShopRequest(
    val name: String? = null,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val logo: String? = null,
    val coverImage: String? = null
) {
    fun validation() {
        validate(this) {
            name?.let {
                validate(UpdateShopRequest::name).isNotEmpty()
            }
            email?.let {
                validate(UpdateShopRequest::email).isNotEmpty()
            }
        }
    }
}
