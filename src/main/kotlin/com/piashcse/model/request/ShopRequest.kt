package com.piashcse.model.request

import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class ShopRequest(
    val name: String,
    val categoryId: String,
    val description: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val logo: String?,
    val coverImage: String?
) {
    fun validation() {
        validate(this) {
            validate(ShopRequest::name).isNotNull().isNotEmpty()
            validate(ShopRequest::categoryId).isNotNull().isNotEmpty()
        }
    }
}
